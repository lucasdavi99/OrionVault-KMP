package com.cuboidestudio.orionvault

import com.cuboidestudio.orionvault.domain.model.VaultConstants
import com.cuboidestudio.orionvault.domain.repository.VaultRepository
import com.cuboidestudio.orionvault.domain.repository.VaultRepositoryImpl
import com.cuboidestudio.orionvault.session.SessionManager
import com.cuboidestudio.orionvault.storage.db.VaultDatabaseProvider
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import com.cuboidestudio.orionvault.storage.secure.createSecureCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Service locator manual (sem DI framework) que conecta storage, crypto e sessão a partir
 * do [PlatformContext] de cada entry point (Android/Desktop/iOS).
 */
class AppContainer(platformContext: PlatformContext) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val secureCredentialStore = createSecureCredentialStore(platformContext)
    private val database = VaultDatabaseProvider.create(platformContext)

    val vaultRepository: VaultRepository = VaultRepositoryImpl(database, secureCredentialStore)

    val sessionManager = SessionManager(
        repository = vaultRepository,
        autoLockTimeoutMillis = VaultConstants.DEFAULT_AUTO_LOCK_TIMEOUT_MINUTES * 60_000L
    ).also { it.startAutoLockWatcher(applicationScope) }
}
