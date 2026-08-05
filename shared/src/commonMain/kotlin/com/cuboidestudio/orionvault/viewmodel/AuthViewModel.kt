package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthMode {
    data object SignUp : AuthMode
    data object Login : AuthMode
}

/**
 * Login/cadastro da conta na nuvem. Segue o padrão StateFlow dos outros ViewModels, com uma
 * adição: um flag [isLoading] — é a primeira tela do app cujo trabalho é ligado à rede, e não
 * uma chamada local praticamente instantânea.
 */
class AuthViewModel(private val container: AppContainer) : ViewModel() {
    private val _mode = MutableStateFlow<AuthMode>(AuthMode.Login)
    val mode: StateFlow<AuthMode> = _mode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Cadastro aceito, mas o Supabase exige confirmação por e-mail antes do primeiro login. */
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    fun toggleMode() {
        _mode.value = if (_mode.value == AuthMode.Login) AuthMode.SignUp else AuthMode.Login
        _errorMessage.value = null
        _infoMessage.value = null
    }

    fun submit(email: String, password: CharArray, confirmPassword: CharArray? = null) {
        if (!email.contains("@") || email.length < 3 || email.startsWith("@") || email.endsWith("@")) {
            _errorMessage.value = "E-mail inválido"
            return
        }
        if (password.size < 8) {
            _errorMessage.value = "A senha deve ter pelo menos 8 caracteres"
            return
        }
        if (_mode.value == AuthMode.SignUp && confirmPassword != null && !password.contentEquals(confirmPassword)) {
            _errorMessage.value = "As senhas não coincidem"
            return
        }
        _errorMessage.value = null
        _infoMessage.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val isSignUp = _mode.value == AuthMode.SignUp
            val result = if (isSignUp) {
                container.accountSessionManager.signUp(email, password)
            } else {
                container.accountSessionManager.login(email, password)
            }
            _isLoading.value = false
            result
                .onSuccess {
                    if (isSignUp && container.accountSessionManager.pendingEmailConfirmation.value) {
                        _infoMessage.value =
                            "Conta criada. Confira sua caixa de entrada e confirme o e-mail antes de entrar."
                        _mode.value = AuthMode.Login
                    } else {
                        _completed.value = true
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Não foi possível concluir a operação" }
        }
    }
}
