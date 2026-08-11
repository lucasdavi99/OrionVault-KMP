package com.cuboidestudio.orionvault

import com.cuboidestudio.orionvault.domain.model.VaultConstants
import com.cuboidestudio.orionvault.domain.repository.VaultRepository
import com.cuboidestudio.orionvault.domain.repository.VaultRepositoryImpl
import com.cuboidestudio.orionvault.network.AuthApiClient
import com.cuboidestudio.orionvault.network.BreachCheckApiClient
import com.cuboidestudio.orionvault.network.SyncApiClient
import com.cuboidestudio.orionvault.network.createHttpClient
import com.cuboidestudio.orionvault.security.PlatformBiometricContext
import com.cuboidestudio.orionvault.security.createBiometricAuthenticator
import com.cuboidestudio.orionvault.session.AccountSessionManager
import com.cuboidestudio.orionvault.session.SessionManager
import com.cuboidestudio.orionvault.storage.db.VaultDatabaseProvider
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import com.cuboidestudio.orionvault.storage.secure.createSecureCredentialStore
import com.cuboidestudio.orionvault.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Service locator manual (sem DI framework) que conecta storage, crypto, sessão e sync a partir
 * do [PlatformContext] de cada entry point (Android/Desktop/iOS).
 */
class AppContainer(platformContext: PlatformContext, platformBiometricContext: PlatformBiometricContext) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    internal val secureCredentialStore = createSecureCredentialStore(platformContext)
    private val database = VaultDatabaseProvider.create(platformContext)

    val biometricAuthenticator = createBiometricAuthenticator(platformBiometricContext, secureCredentialStore)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val httpClient = createHttpClient(json)
    private val authApiClient = AuthApiClient(httpClient, json)

    val accountSessionManager = AccountSessionManager(authApiClient, secureCredentialStore)
        .also { manager -> applicationScope.launch { manager.restoreSession() } }

    // `internal` (mesmo tratamento de `secureCredentialStore`): a OnboardingViewModel precisa
    // buscar os parâmetros de KDF da conta antes de existir um cofre local para restaurar.
    internal val syncApiClient = SyncApiClient(httpClient, json, accountSessionManager)

    internal val breachCheckApiClient = BreachCheckApiClient(httpClient)

    /**
     * Indireção mutável para quebrar a dependência circular entre repositório e motor de sync:
     * o repositório precisa notificar mudanças; o motor precisa ler o repositório.
     */
    private var syncChangeListener: (() -> Unit)? = null

    val vaultRepository: VaultRepository = VaultRepositoryImpl(
        database = database,
        secureStore = secureCredentialStore,
        accountSessionManager = accountSessionManager,
        onLocalChange = { syncChangeListener?.invoke() }
    )

    val syncEngine = SyncEngine(vaultRepository, accountSessionManager, syncApiClient, applicationScope)
        .also { engine -> syncChangeListener = engine::notifyLocalChange }

    val sessionManager = SessionManager(
        repository = vaultRepository,
        autoLockTimeoutMillis = VaultConstants.DEFAULT_AUTO_LOCK_TIMEOUT_MINUTES * 60_000L
    ).also { it.startAutoLockWatcher(applicationScope) }
}
