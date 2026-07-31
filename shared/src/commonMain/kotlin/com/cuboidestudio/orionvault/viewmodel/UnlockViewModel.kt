package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UnlockViewModel(private val container: AppContainer) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun attemptUnlock(masterPassword: CharArray) {
        viewModelScope.launch {
            val success = container.vaultRepository.unlock(masterPassword)
            if (success) {
                container.sessionManager.markUnlocked()
                _errorMessage.value = null
                _unlocked.value = true
            } else {
                _errorMessage.value = "Master Password incorreta"
            }
        }
    }
}
