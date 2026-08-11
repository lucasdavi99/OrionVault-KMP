package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.crypto.KdfParams
import com.cuboidestudio.orionvault.crypto.SecretKeyGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OnboardingStep {
    /** Primeira decisão: este é o primeiro dispositivo, ou já existe um cofre em outro? */
    data object ChooseMode : OnboardingStep

    // --- Caminho "criar cofre novo" ---
    data object SetMasterPassword : OnboardingStep
    data class ShowSecretKey(val secretKeyDisplay: String) : OnboardingStep

    // --- Caminho "restaurar de outro dispositivo" (design doc seção 3.4) ---

    /** Login na conta de nuvem: é de lá que vêm os parâmetros de KDF do cofre original. */
    data object RestoreLogin : OnboardingStep

    /** Master Password + Secret Key anotada, para rederivar exatamente a chave do dispositivo 1. */
    data object RestoreVaultSecrets : OnboardingStep
}

/**
 * Onboarding do cofre, em dois caminhos:
 *
 * - **criar**: define a Master Password, gera a Secret Key e a exibe uma única vez;
 * - **restaurar**: entra na conta de nuvem, baixa os parâmetros de KDF publicados pelo primeiro
 *   dispositivo e rederiva a mesma chave a partir da Master Password + Secret Key digitadas.
 *   Sem esses parâmetros o salt seria outro e nada do que o sync baixasse decifraria.
 */
class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    private val _step = MutableStateFlow<OnboardingStep>(OnboardingStep.ChooseMode)
    val step: StateFlow<OnboardingStep> = _step.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Mesma exceção que a [AuthViewModel] abre: o passo de restauração depende de rede. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    /** Parâmetros baixados em [restoreLogin], consumidos por [restoreVault]. */
    private var fetchedParams: KdfParams? = null

    fun chooseCreate() {
        _errorMessage.value = null
        _step.value = OnboardingStep.SetMasterPassword
    }

    fun chooseRestore() {
        _errorMessage.value = null
        _step.value = OnboardingStep.RestoreLogin
    }

    fun backToChooseMode() {
        _errorMessage.value = null
        _step.value = OnboardingStep.ChooseMode
    }

    fun createVault(masterPassword: CharArray, confirmPassword: CharArray) {
        if (!validatePasswords(masterPassword, confirmPassword)) return
        viewModelScope.launch {
            val secretKeyDisplay = container.vaultRepository.createVault(masterPassword)
            container.sessionManager.markUnlocked()
            _step.value = OnboardingStep.ShowSecretKey(secretKeyDisplay)
        }
    }

    fun confirmSecretKeySaved() {
        _completed.value = true
    }

    /** Credenciais da **conta na nuvem** — nada a ver com a Master Password do cofre. */
    fun restoreLogin(email: String, password: CharArray) {
        if (!email.contains("@") || email.length < 3 || email.startsWith("@") || email.endsWith("@")) {
            _errorMessage.value = "E-mail inválido"
            return
        }
        _errorMessage.value = null
        _isLoading.value = true
        viewModelScope.launch {
            container.accountSessionManager.login(email, password)
                .onFailure { _errorMessage.value = it.message ?: "Não foi possível entrar na conta" }
                .onSuccess {
                    val userId = container.accountSessionManager.currentUserId
                    if (userId == null) {
                        _errorMessage.value = "Não foi possível entrar na conta"
                        return@onSuccess
                    }
                    runCatching { container.syncApiClient.fetchVaultKdf(userId) }
                        .onFailure {
                            _errorMessage.value = it.message ?: "Falha ao consultar o cofre desta conta"
                        }
                        .onSuccess { params ->
                            if (params == null) {
                                _errorMessage.value = "Nenhum cofre encontrado para esta conta neste " +
                                    "servidor. Se este é seu primeiro dispositivo, crie um cofre novo."
                            } else {
                                fetchedParams = params
                                _step.value = OnboardingStep.RestoreVaultSecrets
                            }
                        }
                }
            _isLoading.value = false
        }
    }

    fun restoreVault(masterPassword: CharArray, confirmPassword: CharArray, secretKeyInput: String) {
        if (!validatePasswords(masterPassword, confirmPassword)) return
        val params = fetchedParams
        if (params == null) {
            _errorMessage.value = "Sessão de restauração perdida. Entre na conta novamente."
            _step.value = OnboardingStep.RestoreLogin
            return
        }
        val secretKey = runCatching { SecretKeyGenerator.parseFromDisplay(secretKeyInput) }.getOrNull()
        if (secretKey == null || secretKey.size != SecretKeyGenerator.SECRET_KEY_BYTES) {
            _errorMessage.value = "Secret Key inválida. Confira o valor anotado (formato AAAA-BBBB-...)."
            return
        }
        val userId = container.accountSessionManager.currentUserId
        if (userId == null) {
            _errorMessage.value = "Sessão de restauração perdida. Entre na conta novamente."
            _step.value = OnboardingStep.RestoreLogin
            return
        }
        _errorMessage.value = null
        _isLoading.value = true
        viewModelScope.launch {
            // Antes de finalizar, tenta decifrar um item real da conta com a chave derivada.
            // Sem essa checagem, uma Master Password ou Secret Key erradas não dão erro nenhum
            // aqui - só produzem um cofre local "válido" cuja chave não bate com a do dispositivo
            // original, e os itens baixados pelo sync sumiriam silenciosamente da lista depois.
            //
            // fetchSampleItem só devolve null de verdade quando a conta não tem nenhum item
            // sincronizado ainda - uma falha de rede precisa continuar sendo um erro explícito
            // aqui, e não ser tratada como "nada para verificar" (senão a restauração termina
            // com uma chave nunca conferida).
            val sampleResult = runCatching { container.syncApiClient.fetchSampleItem(userId) }
            val sample = sampleResult.getOrElse {
                _errorMessage.value = it.message ?: "Falha ao verificar a chave com o servidor. Tente novamente."
                _isLoading.value = false
                return@launch
            }
            if (sample != null) {
                val matches = container.vaultRepository.verifyKeyAgainstCiphertext(
                    masterPassword, secretKey, params, sample.passwordCipher
                )
                if (!matches) {
                    _errorMessage.value = "Master Password ou Secret Key incorretas — não batem " +
                        "com o cofre desta conta."
                    _isLoading.value = false
                    return@launch
                }
            }
            container.vaultRepository.restoreVault(masterPassword, secretKey, params)
            container.sessionManager.markUnlocked()
            _isLoading.value = false
            _completed.value = true
        }
    }

    private fun validatePasswords(masterPassword: CharArray, confirmPassword: CharArray): Boolean {
        if (!masterPassword.contentEquals(confirmPassword)) {
            _errorMessage.value = "As senhas não coincidem"
            return false
        }
        if (masterPassword.size < 8) {
            _errorMessage.value = "A Master Password deve ter pelo menos 8 caracteres"
            return false
        }
        _errorMessage.value = null
        return true
    }
}
