package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.security.BiometricAvailability
import com.cuboidestudio.orionvault.storage.secure.BiometricUnlockChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(private val container: AppContainer) : ViewModel() {
    val availability: BiometricAvailability = container.biometricAuthenticator.availability()

    private val _choice = MutableStateFlow(BiometricUnlockChoice.UNDECIDED)
    val choice: StateFlow<BiometricUnlockChoice> = _choice.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _choice.value = container.secureCredentialStore.loadBiometricChoice()
        }
    }

    /** Só funciona com o cofre destravado — a tela de Segurança só é alcançável a partir do cofre. */
    fun enable() {
        viewModelScope.launch {
            val key = container.vaultRepository.currentSessionKeyOrNull()
            if (key == null) {
                _errorMessage.value = "O cofre precisa estar destravado"
                return@launch
            }
            val ok = container.biometricAuthenticator.enroll(key)
            _errorMessage.value = if (ok) null else "Não foi possível ativar a biometria"
            _choice.value = container.secureCredentialStore.loadBiometricChoice()
        }
    }

    fun disable() {
        viewModelScope.launch {
            container.biometricAuthenticator.disable()
            container.secureCredentialStore.saveBiometricChoice(BiometricUnlockChoice.DECLINED)
            _choice.value = BiometricUnlockChoice.DECLINED
        }
    }
}
