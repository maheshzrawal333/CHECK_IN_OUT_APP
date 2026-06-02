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

    // 🌟 The NEW Enterprise Activation Method
    fun activateWithCode(activationCode: String) {
        viewModelScope.launch {
            _uiState.value = RegistrationState.Loading
            try {
                // 1. Generate a unique Hardware ID for this specific phone installation
                val hardwareId = UUID.randomUUID().toString()

                // 2. 🌟 PRO FIX: Move heavy Keystore cryptography to the IO Background Thread
                // This prevents the 1.4-second UI freeze (Choreographer skipped frames) we saw in your logs.
                val publicKey = withContext(Dispatchers.IO) {
                    FingerprintSecurityManager.generateStrictBiometricKey(hardwareId)
                    FingerprintSecurityManager.getPublicKeyBase64(hardwareId)
                }

                // 3. Send the code, the public key, and the hardware ID to Spring Boot
                val response = authRepository.activateDeviceWithCode(
                    activationCode.trim().uppercase(),
                    publicKey,
                    hardwareId
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    // 🌟 ENTERPRISE FIX: Dynamically pull the real user and company names from the server!
                    // If the backend doesn't return them yet, it safely falls back to a default value.
                    val realName = responseBody?.get("fullName") ?: "Unknown Employee"
                    val realOrg = responseBody?.get("orgName") ?: (activationCode.split("-").firstOrNull() ?: "Company")

                    // 4. Save the real data locally so the Check-In screen knows exactly who we are
                    dataStoreManager.saveOrgCode(realOrg)
                    dataStoreManager.saveEmployeeCode(hardwareId)
                    dataStoreManager.saveFullName(realName)

                    // 5. Trigger success to move to the Scanner Ring Screen
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