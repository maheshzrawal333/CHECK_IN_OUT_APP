package com.maheshz.checkinout.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.checkinout.ble.FpPacketAdvertiser
import com.maheshz.checkinout.ble.IotBeaconScanner
import com.maheshz.checkinout.ble.ResultReceiver
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
    object Scanning : HomeState()
    data class Found(val deviceId: String) : HomeState()
    data class Broadcasting(val timeRemaining: Int) : HomeState()
    data class Success(val eventType: String, val timeMs: Long) : HomeState()
    data class Failed(val failureCode: String) : HomeState()
    object Timeout : HomeState()
    data class SecurityLockout(val reason: String) : HomeState()
}

class CheckInViewModel(
    private val scanner: IotBeaconScanner,
    private val advertiser: FpPacketAdvertiser,
    private val resultReceiver: ResultReceiver, // Kept to not break MainActivity DI, but no longer used
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
            // Fetch UI Name
            dataStoreManager.fullNameFlow.collect { name ->
                if (!name.isNullOrEmpty()) _empName.value = name
            }
        }
        viewModelScope.launch {
            // 🌟 CRITICAL FIX: The Keystore alias MUST be the hardware ID (Employee Code), not the name!
            dataStoreManager.employeeCodeFlow.collect { empCode ->
                if (!empCode.isNullOrEmpty()) {
                    currentKeyAlias = empCode
                }
            }
        }
    }

    fun startFlow() {
        val alias = currentKeyAlias ?: return

        when (val result = FingerprintSecurityManager.getStrictSignatureObject(alias)) {
            is FingerprintSecurityManager.SignatureResult.Ready -> {
                viewModelScope.launch {
                    _uiState.value = HomeState.Scanning
                    val orgCode = dataStoreManager.orgCodeFlow.firstOrNull() ?: return@launch
                    scanner.startScanning(orgCode) { deviceId ->
                        _uiState.value = HomeState.Found(deviceId)
                    }
                }
            }
            is FingerprintSecurityManager.SignatureResult.Invalidated -> {
                _uiState.value = HomeState.SecurityLockout(
                    "SECURITY BREACH: The biometric fingerprints on this device were recently changed. " +
                            "Your access key has been permanently destroyed to protect corporate data."
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

            // 1. Generate Cryptographic Payload
            val timestamp = System.currentTimeMillis() / 1000
            val dataToSign = "$empCode:$timestamp".toByteArray(StandardCharsets.UTF_8)
            signatureObject.update(dataToSign)
            val signatureBytes = signatureObject.sign()

            // 2. Start Broadcasting (Fire and Forget)
            _uiState.value = HomeState.Broadcasting(10)
            advertiser.startAdvertising(empCode, timestamp, signatureBytes) { }

            // 3. 🌟 ENTERPRISE HYBRID POLLING LOOP 🌟
            // The phone will check the server for up to 10 seconds to see if the IoT Gateway processed the scan.
            val result = withTimeoutOrNull(10_000L) {
                var verifiedEventType: String? = null

                while (verifiedEventType == null) {
                    try {
                        val response = attendanceRepository.checkLatestScanStatus(empCode)

                        if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                            // Backend confirms! It will explicitly tell us if this was a CHECK_IN or CHECK_OUT
                            verifiedEventType = response.body()?.eventType
                        }
                    } catch (e: Exception) {
                        // Ignore network drops, just wait and try again
                    }

                    delay(1500) // Poll every 1.5 seconds
                }
                verifiedEventType // Returns the string ("CHECK_IN" or "CHECK_OUT") when found
            }

            // 4. Update UI Based on Server Truth
            if (result != null) {
                val time = System.currentTimeMillis()

                // Show the specific check-in or check-out success message
                _uiState.value = HomeState.Success(result, time)

                // Log locally for the history screen
                attendanceRepository.insertRecord(AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    type = result,
                    timestamp = time,
                    verifiedBy = "SYSTEM"
                ))
            } else {
                // The 10 seconds expired. The scanner didn't hear us or the server is down.
                _uiState.value = HomeState.Timeout
            }
        }
    }

    fun resetState() {
        _uiState.value = HomeState.Idle
    }

    companion object {
        fun provideFactory(
            scanner: IotBeaconScanner, advertiser: FpPacketAdvertiser,
            resultReceiver: ResultReceiver, dataStoreManager: DataStoreManager,
            attendanceRepository: AttendanceRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CheckInViewModel(scanner, advertiser, resultReceiver, dataStoreManager, attendanceRepository) as T
            }
        }
    }
}