package com.cuboidestudio.orionvault.session

import com.cuboidestudio.orionvault.crypto.KdfParams
import com.cuboidestudio.orionvault.domain.model.SyncFolder
import com.cuboidestudio.orionvault.domain.model.SyncItem
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import com.cuboidestudio.orionvault.domain.repository.VaultRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeTimeProvider(var current: Long = 0L) : TimeProvider {
    override fun nowMillis(): Long = current
}

/** Repositório fake só para observar chamadas de lock() sem depender de crypto/storage reais. */
private class FakeVaultRepository : VaultRepository {
    var locked = false
        private set
    private var unlocked = false

    override suspend fun isVaultInitialized(): Boolean = true
    override suspend fun createVault(masterPassword: CharArray): String = ""
    override suspend fun restoreVault(masterPassword: CharArray, secretKey: ByteArray, params: KdfParams) {}
    override suspend fun verifyKeyAgainstCiphertext(
        masterPassword: CharArray,
        secretKey: ByteArray,
        params: KdfParams,
        sampleCipher: String
    ): Boolean = true
    override suspend fun unlock(masterPassword: CharArray): Boolean {
        unlocked = true
        return true
    }

    override suspend fun unlockWithKey(key: ByteArray): Boolean {
        unlocked = true
        return true
    }

    override fun currentSessionKeyOrNull(): ByteArray? = if (unlocked) ByteArray(0) else null

    override fun lock() {
        locked = true
        unlocked = false
    }

    override fun isUnlocked(): Boolean = unlocked

    override suspend fun listFolders(parentId: String?): List<VaultFolder> = emptyList()
    override suspend fun listAllFolders(): List<VaultFolder> = emptyList()
    override suspend fun createFolder(parentId: String?, name: String): VaultFolder = error("not used")
    override suspend fun renameFolder(id: String, newName: String) {}
    override suspend fun deleteFolder(id: String) {}
    override suspend fun listItems(folderId: String?): List<VaultItem> = emptyList()
    override suspend fun createItem(
        folderId: String?,
        title: String,
        username: String?,
        email: String?,
        password: String,
        url: String?,
        notes: String?
    ): VaultItem = error("not used")

    override suspend fun updateItem(
        id: String,
        folderId: String?,
        title: String,
        username: String?,
        email: String?,
        password: String,
        url: String?,
        notes: String?
    ) {
    }

    override suspend fun deleteItem(id: String) {}

    override suspend fun listDirtyFolders(): List<SyncFolder> = emptyList()
    override suspend fun listDirtyItems(): List<SyncItem> = emptyList()
    override suspend fun markFolderSynced(id: String, newVersion: Long) {}
    override suspend fun markItemSynced(id: String, newVersion: Long) {}
    override suspend fun purgeTombstone(id: String, isFolder: Boolean) {}
    override suspend fun applyRemoteFolder(remote: SyncFolder) {}
    override suspend fun applyRemoteItem(remote: SyncItem) {}
    override suspend fun getSyncCursor(): Long? = null
    override suspend fun setSyncCursor(timestamp: Long) {}
    override suspend fun getOrCreateDeviceId(): String = "test-device"
    override suspend fun getLocalKdfParams(): KdfParams? = null
}

class SessionManagerTest {

    @Test
    fun checkAutoLockLocksAfterTimeoutWithoutActivity() {
        val time = FakeTimeProvider(0L)
        val repository = FakeVaultRepository()
        val session = SessionManager(repository, autoLockTimeoutMillis = 1_000L, timeProvider = time)

        session.markUnlocked()
        assertTrue(session.isUnlocked.value)

        time.current = 999L
        session.checkAutoLock()
        assertFalse(repository.locked)
        assertTrue(session.isUnlocked.value)

        time.current = 1_000L
        session.checkAutoLock()
        assertTrue(repository.locked)
        assertFalse(session.isUnlocked.value)
    }

    @Test
    fun recordActivityPostponesAutoLock() {
        val time = FakeTimeProvider(0L)
        val repository = FakeVaultRepository()
        val session = SessionManager(repository, autoLockTimeoutMillis = 1_000L, timeProvider = time)

        session.markUnlocked()

        time.current = 900L
        session.recordActivity()

        time.current = 1_800L
        session.checkAutoLock()
        assertFalse(repository.locked, "não deveria bloquear: só se passaram 900ms desde a última atividade")

        time.current = 1_901L
        session.checkAutoLock()
        assertTrue(repository.locked)
    }

    @Test
    fun checkAutoLockIsNoOpWhenAlreadyLocked() {
        val time = FakeTimeProvider(0L)
        val repository = FakeVaultRepository()
        val session = SessionManager(repository, autoLockTimeoutMillis = 1_000L, timeProvider = time)

        time.current = 5_000L
        session.checkAutoLock()

        assertFalse(repository.locked)
        assertFalse(session.isUnlocked.value)
    }
}
