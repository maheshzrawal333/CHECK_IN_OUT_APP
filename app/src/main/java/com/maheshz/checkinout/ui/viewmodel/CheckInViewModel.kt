package com.maheshz.checkinout.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.checkinout.ble.FpPacketAdvertiser
import com.maheshz.checkinout.data.repository.AttendanceRepository
import com.maheshz.checkinout.model.AttendanceRecord
import com.maheshz.checkinout.util.DataStoreManager
import com.maheshz.checkinout.util.FingerprintSecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets
import java.security.Signature
import java.util.UUID

sealed class HomeState {
    object Idle : HomeState()
    data class Found(val deviceId: String) : HomeState()
    data class Broadcasting(val timeRemaining: Int) : HomeState()
    data class Success(val eventType: String, val timeMs: Long) : HomeState()
    data class Failed(val failureCode: String) : HomeState()
    object Timeout : HomeState()
    data class SecurityLockout(val reason: String) : HomeState()
}

class CheckInViewModel(
    private val advertiser: FpPacketAdvertiser,
    private val dataStoreManager: DataStoreManager,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeState>(HomeState.Idle)
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _empName = MutableStateFlow<String>("")
    val empName: StateFlow<String> = _empName.asStateFlow()

    private var currentKeyAlias: String? = null

    init {
        viewModelScope.launch {
            dataStoreManager.fullNameFlow.collect { name ->
                if (!name.isNullOrEmpty()) _empName.value = name
            }
        }
        viewModelScope.launch {
            dataStoreManager.employeeCodeFlow.collect { empCode ->
                if (!empCode.isNullOrEmpty()) currentKeyAlias = empCode
            }
        }
    }

    fun startFlow() {
        val alias = currentKeyAlias ?: return

        when (val result = FingerprintSecurityManager.getStrictSignatureObject(alias)) {
            is FingerprintSecurityManager.SignatureResult.Ready -> {
                _uiState.value = HomeState.Found("KIOSK")
            }
            is FingerprintSecurityManager.SignatureResult.Invalidated -> {
                _uiState.value = HomeState.SecurityLockout(
                    "SECURITY BREACH: The biometric fingerprints on this device were recently changed. " +
                            "Your access key has been permanently destroyed."
                )
            }
            is FingerprintSecurityManager.SignatureResult.Error -> {
                _uiState.value = HomeState.Failed(result.message)
            }
        }
    }

    fun getSignatureObject(): Signature? {
        val alias = currentKeyAlias ?: return null
        val result = FingerprintSecurityManager.getStrictSignatureObject(alias)
        return if (result is FingerprintSecurityManager.SignatureResult.Ready) result.signature else null
    }

    fun onFingerprintSuccess(signatureObject: Signature) {
        viewModelScope.launch {
            val empCode = dataStoreManager.employeeCodeFlow.firstOrNull() ?: return@launch

            val timestamp = System.currentTimeMillis() / 1000
            val dataToSign = "$empCode:$timestamp".toByteArray(StandardCharsets.UTF_8)
            signatureObject.update(dataToSign)
            val signatureBytes = signatureObject.sign()

            _uiState.value = HomeState.Broadcasting(10)
            advertiser.startAdvertising(empCode, timestamp, signatureBytes) { }

            // Safe Polling Mechanism preventing overlapping requests
            val result = withTimeoutOrNull(10_000L) {
                var verifiedEventType: String? = null
                var isRequestInFlight = false

                while (verifiedEventType == null) {
                    if (!isRequestInFlight) {
                        isRequestInFlight = true
                        try {
                            val response = attendanceRepository.checkLatestScanStatus(empCode)
                            if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                                verifiedEventType = response.body()?.eventType
                            }
                        } catch (e: Exception) {
                            // Ignore network blips during polling
                        } finally {
                            isRequestInFlight = false
                        }
                    }
                    delay(1500)
                }
                verifiedEventType
            }

            if (result != null) {
                val time = System.currentTimeMillis()
                _uiState.value = HomeState.Success(result, time)
                attendanceRepository.insertRecord(AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    type = result,
                    timestamp = time,
                    verifiedBy = "SYSTEM"
                ))
            } else {
                _uiState.value = HomeState.Timeout
            }
        }
    }

    fun resetState() { _uiState.value = HomeState.Idle }

    companion object {
        fun provideFactory(
            advertiser: FpPacketAdvertiser,
            dataStoreManager: DataStoreManager,
            attendanceRepository: AttendanceRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CheckInViewModel(advertiser, dataStoreManager, attendanceRepository) as T
            }
        }
    }
}