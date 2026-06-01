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
import com.maheshz.checkinout.util.FingerprintSecurityManager // 🌟 ADDED: Your new security manager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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

    // 🌟 ADDED: New state to handle biometric database tampering
    data class SecurityLockout(val reason: String) : HomeState()
}

class CheckInViewModel(
    private val scanner: IotBeaconScanner,
    private val advertiser: FpPacketAdvertiser,
    private val resultReceiver: ResultReceiver,
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
                if (!name.isNullOrEmpty()) {
                    _empName.value = name
                    currentKeyAlias = name.replace("\\s+".toRegex(), "").lowercase()
                }
            }
        }
    }

    fun startFlow() {
        val alias = currentKeyAlias ?: return

        // 🌟 PROFESSIONAL FIX: Check cryptographic key integrity BEFORE scanning
        when (val result = FingerprintSecurityManager.getStrictSignatureObject(alias)) {
            is FingerprintSecurityManager.SignatureResult.Ready -> {
                // Key is intact and secure. Proceed with BLE Scanning!
                viewModelScope.launch {
                    _uiState.value = HomeState.Scanning
                    val orgCode = dataStoreManager.orgCodeFlow.firstOrNull() ?: return@launch
                    scanner.startScanning(orgCode) { deviceId ->
                        _uiState.value = HomeState.Found(deviceId)
                    }
                }
            }
            is FingerprintSecurityManager.SignatureResult.Invalidated -> {
                // 🚨 SECURITY BREACH DETECTED: Someone altered the device fingerprints!
                _uiState.value = HomeState.SecurityLockout(
                    "SECURITY BREACH: The biometric fingerprints on this device were recently changed. " +
                            "Your access key has been permanently destroyed to protect corporate data. " +
                            "Please contact your Administrator to wipe your profile and re-register this device."
                )
            }
            is FingerprintSecurityManager.SignatureResult.Error -> {
                _uiState.value = HomeState.Failed(result.message)
            }
        }
    }

    // Called by the UI when the device is found to pass to the BiometricPrompt
    fun getSignatureObject(): Signature? {
        val alias = currentKeyAlias ?: return null
        val result = FingerprintSecurityManager.getStrictSignatureObject(alias)
        return if (result is FingerprintSecurityManager.SignatureResult.Ready) {
            result.signature
        } else {
            null
        }
    }

    fun onFingerprintSuccess(signatureObject: Signature) {
        viewModelScope.launch {
            val empCode = dataStoreManager.employeeCodeFlow.firstOrNull() ?: return@launch
            _uiState.value = HomeState.Broadcasting(10)

            val timestamp = System.currentTimeMillis() / 1000
            val dataToSign = "$empCode:$timestamp".toByteArray(StandardCharsets.UTF_8)

            signatureObject.update(dataToSign)
            val signatureBytes = signatureObject.sign()

            advertiser.startAdvertising(empCode, timestamp, signatureBytes) { }

            val result = resultReceiver.waitForResult(empCode)
            if (result == null) {
                _uiState.value = HomeState.Timeout
            } else if (result.success) {
                val time = System.currentTimeMillis()
                _uiState.value = HomeState.Success(result.message, time)
                attendanceRepository.insertRecord(AttendanceRecord(
                    id = UUID.randomUUID().toString(), type = result.message, timestamp = time, verifiedBy = "SYSTEM"
                ))
            } else {
                _uiState.value = HomeState.Failed(result.message)
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