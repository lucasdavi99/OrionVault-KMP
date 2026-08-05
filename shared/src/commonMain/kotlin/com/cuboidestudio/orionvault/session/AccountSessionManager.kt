@file:OptIn(ExperimentalTime::class)

package com.cuboidestudio.orionvault.session

import com.cuboidestudio.orionvault.network.AuthApiClient
import com.cuboidestudio.orionvault.network.AuthSession
import com.cuboidestudio.orionvault.network.SignUpResult
import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
import com.cuboidestudio.orionvault.storage.secure.StoredAuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface AccountState {
    data object LoggedOut : AccountState
    data object LoggingIn : AccountState
    data class LoggedIn(val email: String, val userId: String) : AccountState
}

/**
 * Sessão da **conta na nuvem** — paralela e deliberadamente independente de [SessionManager]
 * (a sessão do cofre local). Nunca são fundidos: o usuário pode destravar o cofre local sem
 * estar logado na nuvem, e um logout da nuvem jamais tranca o cofre local.
 */
class AccountSessionManager(
    private val authApiClient: AuthApiClient,
    private val secureStore: SecureCredentialStore
) {
    private val _accountState = MutableStateFlow<AccountState>(AccountState.LoggedOut)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    /** Última mensagem informativa (ex.: "confirme seu e-mail") produzida por um signUp. */
    private val _pendingEmailConfirmation = MutableStateFlow(false)
    val pendingEmailConfirmation: StateFlow<Boolean> = _pendingEmailConfirmation.asStateFlow()

    private val tokenMutex = Mutex()
    private var session: AuthSession? = null

    /** Chamado uma vez na inicialização do [com.cuboidestudio.orionvault.AppContainer]. */
    suspend fun restoreSession() {
        val stored = secureStore.loadAuthSession() ?: return
        val restored = AuthSession(
            accessToken = stored.accessToken,
            refreshToken = stored.refreshToken,
            expiresAtEpochMillis = stored.expiresAtEpochMillis,
            userId = stored.userId,
            email = stored.email
        )
        session = restored
        _accountState.value = AccountState.LoggedIn(restored.email, restored.userId)
        // Renova proativamente se já estiver perto de expirar; se o refresh falhar (token
        // revogado), cai para LoggedOut sem tocar no cofre local.
        if (isNearExpiry(restored)) {
            tokenMutex.withLock { refreshLocked() }
        }
    }

    suspend fun signUp(email: String, password: CharArray): Result<Unit> {
        _accountState.value = AccountState.LoggingIn
        _pendingEmailConfirmation.value = false
        return authApiClient.signUp(email, password)
            .onSuccess { result ->
                when (result) {
                    is SignUpResult.SignedIn -> persist(result.session)
                    SignUpResult.ConfirmationRequired -> {
                        _pendingEmailConfirmation.value = true
                        _accountState.value = AccountState.LoggedOut
                    }
                }
            }
            .onFailure { _accountState.value = AccountState.LoggedOut }
            .map { }
    }

    suspend fun login(email: String, password: CharArray): Result<Unit> {
        _accountState.value = AccountState.LoggingIn
        _pendingEmailConfirmation.value = false
        return authApiClient.login(email, password)
            .onSuccess { persist(it) }
            .onFailure { _accountState.value = AccountState.LoggedOut }
            .map { }
    }

    /** Encerra apenas a sessão de nuvem. Não toca em [SessionManager] nem no cofre local. */
    suspend fun logout() {
        val current = session
        session = null
        _accountState.value = AccountState.LoggedOut
        _pendingEmailConfirmation.value = false
        secureStore.clearAuthSession()
        if (current != null) authApiClient.logout(current.accessToken)
    }

    val currentUserId: String? get() = (accountState.value as? AccountState.LoggedIn)?.userId

    /** Token de acesso válido, renovando de forma transparente se estiver expirado/perto disso. */
    suspend fun currentAccessToken(): String? = tokenMutex.withLock {
        val current = session ?: return null
        if (!isNearExpiry(current)) return current.accessToken
        refreshLocked()
        session?.accessToken
    }

    private suspend fun refreshLocked() {
        val current = session ?: return
        authApiClient.refresh(current.refreshToken)
            .onSuccess { refreshed ->
                // O endpoint de refresh nem sempre devolve o e-mail; preserva o conhecido.
                persist(refreshed.copy(email = refreshed.email.ifBlank { current.email }))
            }
            .onFailure {
                session = null
                _accountState.value = AccountState.LoggedOut
                secureStore.clearAuthSession()
            }
    }

    private suspend fun persist(newSession: AuthSession) {
        session = newSession
        secureStore.saveAuthSession(
            StoredAuthSession(
                accessToken = newSession.accessToken,
                refreshToken = newSession.refreshToken,
                expiresAtEpochMillis = newSession.expiresAtEpochMillis,
                userId = newSession.userId,
                email = newSession.email
            )
        )
        _accountState.value = AccountState.LoggedIn(newSession.email, newSession.userId)
    }

    private fun isNearExpiry(candidate: AuthSession): Boolean =
        Clock.System.now().toEpochMilliseconds() >= candidate.expiresAtEpochMillis - EXPIRY_SKEW_MILLIS

    private companion object {
        const val EXPIRY_SKEW_MILLIS = 60_000L
    }
}
