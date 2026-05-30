package com.maheshz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.ble.FpPacketAdvertiser
import com.maheshz.ble.IotBeaconScanner
import com.maheshz.ble.ResultReceiver
import com.maheshz.biometric.FingerprintSigner
import com.maheshz.data.repository.AttendanceRepository
import com.maheshz.model.AttendanceRecord
import com.maheshz.util.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
                    // Pre-calculate the safe hardware alias name asynchronously
                    currentKeyAlias = name.replace("\\s+".toRegex(), "").lowercase()
                }
            }
        }
    }

    fun startFlow() {
        viewModelScope.launch {
            _uiState.value = HomeState.Scanning
            val orgCode = dataStoreManager.orgCodeFlow.firstOrNull() ?: return@launch
            scanner.startScanning(orgCode) { deviceId ->
                _uiState.value = HomeState.Found(deviceId)
            }
        }
    }

    fun getSignatureObject(): Signature? {
        val alias = currentKeyAlias ?: return null
        return FingerprintSigner.getSignatureObject(alias)
    }

    fun onFingerprintSuccess(signature: ByteArray) {
        viewModelScope.launch {
            val empCode = dataStoreManager.employeeCodeFlow.firstOrNull() ?: return@launch
            _uiState.value = HomeState.Broadcasting(10)

            val timestamp = System.currentTimeMillis() / 1000
            val dataToSign = "${empCode}:${timestamp}".toByteArray()

            advertiser.startAdvertising(empCode, signature) {
            }

            val result = resultReceiver.waitForResult(empCode)
            if (result == null) {
                _uiState.value = HomeState.Timeout
            } else if (result.success) {
                val time = System.currentTimeMillis()
                _uiState.value = HomeState.Success(result.message, time)
                attendanceRepository.insertRecord(AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    type = result.message,
                    timestamp = time,
                    verifiedBy = "SYSTEM"
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
