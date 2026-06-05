package com.maheshz.checkinout.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.checkinout.data.repository.AuthRepository
import com.maheshz.checkinout.util.DataStoreManager
import com.maheshz.checkinout.util.FingerprintSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class RegistrationViewModel(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val uiState: StateFlow<RegistrationState> = _uiState.asStateFlow()

    fun activateWithCode(activationCode: String) {
        viewModelScope.launch {
            _uiState.value = RegistrationState.Loading
            try {
                // Generate a unique Hardware ID for this specific phone installation
                val hardwareId = UUID.randomUUID().toString()

                // Move heavy Keystore cryptography to the IO Background Thread
                // This prevents the 1.4-second UI freeze (Choreographer skipped frames).
                val publicKey = withContext(Dispatchers.IO) {
                    FingerprintSecurityManager.generateStrictBiometricKey(hardwareId)
                    FingerprintSecurityManager.getPublicKeyBase64(hardwareId)
                }

                // Send the code, the public key, and the hardware ID to Spring Boot
                val response = authRepository.activateDeviceWithCode(
                    activationCode.trim().uppercase(),
                    publicKey,
                    hardwareId
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    // Dynamically pull the real user and company names from the server!
                    val realName = responseBody?.get("fullName") ?: "Unknown Employee"
                    val realOrgCode = responseBody?.get("orgCode") ?: (activationCode.split("-").firstOrNull() ?: "UNKNOWN")

                    // Extract the JWT tokens provided by the Server
                    val accessToken = responseBody?.get("access_token") ?: ""
                    val refreshToken = responseBody?.get("refresh_token") ?: ""

                    // Save the real data locally so the Check-In screen knows exactly who we are
                    dataStoreManager.saveOrgCode(realOrgCode)
                    dataStoreManager.saveEmployeeCode(hardwareId)
                    dataStoreManager.saveFullName(realName)

                    // Save the tokens! Without this, the AI Chat gets a 403 Forbidden.
                    dataStoreManager.saveTokens(accessToken, refreshToken)

                    // Trigger success to move to the Scanner Ring Screen
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

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}