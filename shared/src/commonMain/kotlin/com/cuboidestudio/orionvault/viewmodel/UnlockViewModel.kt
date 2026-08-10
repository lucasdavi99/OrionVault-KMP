package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.storage.secure.BiometricUnlockChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UnlockViewModel(private val container: AppContainer) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /** Convite pós-unlock estilo banco: "usar biometria do celular?" — só dispara uma vez, quando ainda indeciso. */
    private val _askBiometricEnable = MutableStateFlow(false)
    val askBiometricEnable: StateFlow<Boolean> = _askBiometricEnable.asStateFlow()

    private val _biometricAvailableForQuickUnlock = MutableStateFlow(false)
    val biometricAvailableForQuickUnlock: StateFlow<Boolean> = _biometricAvailableForQuickUnlock.asStateFlow()

    init {
        refreshQuickUnlockAvailability()
    }

    fun refreshQuickUnlockAvailability() {
        viewModelScope.launch {
            val choice = container.secureCredentialStore.loadBiometricChoice()
            _biometricAvailableForQuickUnlock.value =
                container.biometricAuthenticator.isAvailable() && choice == BiometricUnlockChoice.ENABLED
        }
    }

    fun attemptUnlock(masterPassword: CharArray) {
        viewModelScope.launch {
            val success = container.vaultRepository.unlock(masterPassword)
            if (success) {
                container.sessionManager.markUnlocked()
                _errorMessage.value = null
                _unlocked.value = true
                maybeAskBiometric()
            } else {
                _errorMessage.value = "Master Password incorreta"
            }
        }
    }

    /** Desbloqueio rápido via biometria/PIN do aparelho, sem digitar a Master Password. */
    fun quickUnlock() {
        viewModelScope.launch {
            val key = container.biometricAuthenticator.promptUnlock() ?: return@launch
            val success = container.vaultRepository.unlockWithKey(key)
            key.fill(0)
            if (success) {
                container.sessionManager.markUnlocked()
                _errorMessage.value = null
                _unlocked.value = true
            } else {
                _errorMessage.value = "Não foi possível validar a chave biométrica"
            }
        }
    }

    fun confirmEnableBiometric() {
        viewModelScope.launch {
            val key = container.vaultRepository.currentSessionKeyOrNull()
            if (key != null) container.biometricAuthenticator.enroll(key)
            _askBiometricEnable.value = false
        }
    }

    fun declineBiometric() {
        viewModelScope.launch {
            container.secureCredentialStore.saveBiometricChoice(BiometricUnlockChoice.DECLINED)
            _askBiometricEnable.value = false
        }
    }

    private suspend fun maybeAskBiometric() {
        if (!container.biometricAuthenticator.isAvailable()) return
        val choice = container.secureCredentialStore.loadBiometricChoice()
        if (choice == BiometricUnlockChoice.UNDECIDED) _askBiometricEnable.value = true
    }
}
