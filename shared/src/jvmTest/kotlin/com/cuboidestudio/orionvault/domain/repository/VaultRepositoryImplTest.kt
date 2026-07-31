package com.cuboidestudio.orionvault.domain.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuboidestudio.orionvault.crypto.ensureLibsodiumInitialized
import com.cuboidestudio.orionvault.domain.model.VaultConstants
import com.cuboidestudio.orionvault.storage.db.OrionVaultDatabase
import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
import com.cuboidestudio.orionvault.storage.secure.StoredVaultSecrets
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Store em memória só para testes — evita depender do secure store real da plataforma. */
private class InMemorySecureCredentialStore : SecureCredentialStore {
    private var stored: StoredVaultSecrets? = null
    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) { stored = secrets }
    override suspend fun loadVaultSecrets(): StoredVaultSecrets? = stored
    override suspend fun clear() { stored = null }
}

class VaultRepositoryImplTest {

    private fun newRepository(): VaultRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        OrionVaultDatabase.Schema.create(driver)
        return VaultRepositoryImpl(OrionVaultDatabase(driver), InMemorySecureCredentialStore())
    }

    @Test
    fun createVaultThenUnlockWithCorrectPassword() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        assertFalse(repo.isVaultInitialized())

        repo.createVault("correct horse".toCharArray())
        assertTrue(repo.isVaultInitialized())
        assertTrue(repo.isUnlocked())

        repo.lock()
        assertFalse(repo.isUnlocked())

        assertTrue(repo.unlock("correct horse".toCharArray()))
        assertTrue(repo.isUnlocked())
    }

    @Test
    fun unlockFailsWithWrongPassword() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        repo.lock()

        assertFalse(repo.unlock("wrong horse".toCharArray()))
        assertFalse(repo.isUnlocked())
    }

    @Test
    fun createFolderAndItemRoundTripsDecryptedCorrectly() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())

        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", "user@example.com", "s3cr3t", "https://gmail.com", "nota")

        val items = repo.listItems(folder.id)
        assertEquals(1, items.size)
        assertEquals("Gmail", items[0].title)
        assertEquals("user@example.com", items[0].username)
        assertEquals("s3cr3t", items[0].password)
        assertEquals("https://gmail.com", items[0].url)
        assertEquals("nota", items[0].notes)
        assertEquals(1, items[0].version)
        assertEquals(item.id, items[0].id)
    }

    @Test
    fun updateItemIncrementsVersionAndChangesCipher() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, "s3cr3t", null, null)

        repo.updateItem(item.id, "Gmail", null, "n0v4s3nh4", null, null)

        val updated = repo.listItems(folder.id).single()
        assertEquals(2, updated.version)
        assertEquals("n0v4s3nh4", updated.password)
    }

    @Test
    fun createFolderRejectsBeyondMaxDepth() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())

        var parentId: String? = null
        repeat(VaultConstants.MAX_FOLDER_DEPTH) {
            parentId = repo.createFolder(parentId, "Nível $it").id
        }

        assertFailsWith<FolderDepthExceededException> {
            repo.createFolder(parentId, "Além do limite")
        }
    }

    @Test
    fun listItemsThrowsWhenLocked() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        repo.lock()

        assertFailsWith<IllegalStateException> {
            repo.listItems(folder.id)
        }
    }
}
