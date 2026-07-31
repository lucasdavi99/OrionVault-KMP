package com.cuboidestudio.orionvault.session

import com.cuboidestudio.orionvault.domain.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controla a sessão do cofre: mantém o estado de unlock reativo para a UI e bloqueia
 * automaticamente após um período de inatividade, zerando a chave em memória via
 * [VaultRepository.lock] (design doc seção 5).
 */
class SessionManager(
    private val repository: VaultRepository,
    private val autoLockTimeoutMillis: Long,
    private val timeProvider: TimeProvider = SystemTimeProvider
) {
    private val _isUnlocked = MutableStateFlow(repository.isUnlocked())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastActivityMillis: Long = timeProvider.nowMillis()

    /** Deve ser chamado depois de um [VaultRepository.createVault]/[VaultRepository.unlock] bem-sucedido. */
    fun markUnlocked() {
        lastActivityMillis = timeProvider.nowMillis()
        _isUnlocked.value = true
    }

    fun lock() {
        repository.lock()
        _isUnlocked.value = false
    }

    /** Deve ser chamado a cada interação relevante do usuário na UI, para adiar o auto-lock. */
    fun recordActivity() {
        lastActivityMillis = timeProvider.nowMillis()
    }

    /** Verifica se o timeout de inatividade expirou e bloqueia se necessário. */
    fun checkAutoLock() {
        if (_isUnlocked.value && timeProvider.nowMillis() - lastActivityMillis >= autoLockTimeoutMillis) {
            lock()
        }
    }

    /** Inicia o watcher periódico de auto-lock num [scope] de longa duração (ex.: escopo da aplicação). */
    fun startAutoLockWatcher(scope: CoroutineScope, checkIntervalMillis: Long = 5_000) {
        scope.launch {
            while (true) {
                delay(checkIntervalMillis)
                checkAutoLock()
            }
        }
    }
}
