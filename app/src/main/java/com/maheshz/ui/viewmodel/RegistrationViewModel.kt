package com.maheshz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.biometric.KeystoreManager
import com.maheshz.data.repository.AuthRepository
import com.maheshz.util.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Locale.getDefault

class RegistrationViewModel(
    private val authRepository: AuthRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val uiState: StateFlow<RegistrationState> = _uiState.asStateFlow()

    fun registerSimplified(orgCode: String, fullName: String, email: String) {
        viewModelScope.launch {
            _uiState.value = RegistrationState.Loading
            try {
                val localKeyAlias = fullName.replace("\\s+".toRegex(), "").lowercase()
                val pubKeyBase64 = KeystoreManager.generateECKeyPair(localKeyAlias)
                val deviceFingerprint = dataStoreManager.getDeviceFingerprint()

                authRepository.registerSimplified(orgCode, fullName, email, pubKeyBase64, deviceFingerprint)

                // Switch state to waiting screen notice layout
                _uiState.value = RegistrationState.WaitingForApproval
            } catch (e: Exception) {
                _uiState.value = RegistrationState.Error(e.message ?: "Submission error")
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
    object WaitingForApproval : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
