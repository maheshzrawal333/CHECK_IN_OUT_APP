package com.maheshz.checkinout.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.checkinout.data.repository.AuthRepository
import com.maheshz.checkinout.util.DataStoreManager
import com.maheshz.checkinout.util.FingerprintSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RegistrationViewModel(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val uiState: StateFlow<RegistrationState> = _uiState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // 🌟 This is the NEW Enterprise Activation Method
    fun activateWithCode(activationCode: String) {
        viewModelScope.launch {
            _uiState.value = RegistrationState.Loading
            try {
                // 1. Generate a unique Hardware ID for this specific phone installation
                val hardwareId = UUID.randomUUID().toString()

                // 2. Generate the Strict Biometric Key (self-destructs if new finger added)
                FingerprintSecurityManager.generateStrictBiometricKey(hardwareId)

                // 3. Extract the Public Key to send to the server
                val publicKey = FingerprintSecurityManager.getPublicKeyBase64(hardwareId)

                // 4. Send the code, the public key, and the hardware ID to Spring Boot
                val response = authRepository.activateDeviceWithCode(
                    activationCode.trim().uppercase(),
                    publicKey,
                    hardwareId
                )

                if (response.isSuccessful) {
                    // 5. Save the data locally so the Check-In screen knows who we are!
                    val extractedOrgCode = activationCode.split("-").firstOrNull() ?: ""

                    dataStoreManager.saveOrgCode(extractedOrgCode)
                    dataStoreManager.saveEmployeeCode(hardwareId)
                    // (You can update the backend later to return the real name in the response)
                    dataStoreManager.saveFullName("Employee")

                    // 6. Trigger success to move to the Scanner Ring Screen
                    _uiState.value = RegistrationState.Success
                } else {
                    _uiState.value = RegistrationState.Error("Invalid or Expired Activation Code.")
                }
            } catch (e: Exception) {
                _uiState.value = RegistrationState.Error("Network error: ${e.message}")
            }
        }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository, dataStoreManager: DataStoreManager): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RegistrationViewModel(authRepository, dataStoreManager) as T
            }
        }
    }
}

// Ensure Success state exists here
sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object WaitingForApproval : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}