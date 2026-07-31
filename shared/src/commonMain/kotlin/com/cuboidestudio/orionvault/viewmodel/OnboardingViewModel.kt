package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OnboardingStep {
    data object SetMasterPassword : OnboardingStep
    data class ShowSecretKey(val secretKeyDisplay: String) : OnboardingStep
}

/** Fluxo de criação do cofre: define a Master Password, gera a Secret Key e exibe uma única vez. */
class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.SetMasterPassword)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun createVault(masterPassword: CharArray, confirmPassword: CharArray) {
        if (!masterPassword.contentEquals(confirmPassword)) {
            _errorMessage.value = "As senhas não coincidem"
            return
        }
        if (masterPassword.size < 8) {
            _errorMessage.value = "A Master Password deve ter pelo menos 8 caracteres"
            return
        }
        _errorMessage.value = null
        viewModelScope.launch {
            val secretKeyDisplay = container.vaultRepository.createVault(masterPassword)
            container.sessionManager.markUnlocked()
            _step.value = OnboardingStep.ShowSecretKey(secretKeyDisplay)
        }
    }

    fun confirmSecretKeySaved() {
        _completed.value = true
    }
}
