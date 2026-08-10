package com.cuboidestudio.orionvault.domain.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuboidestudio.orionvault.crypto.SecretKeyGenerator
import com.cuboidestudio.orionvault.crypto.ensureLibsodiumInitialized
import com.cuboidestudio.orionvault.domain.model.VaultConstants
import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.cuboidestudio.orionvault.storage.db.OrionVaultDatabase
import com.cuboidestudio.orionvault.domain.model.SyncState
import com.cuboidestudio.orionvault.storage.secure.BiometricUnlockChoice
import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
import com.cuboidestudio.orionvault.storage.secure.StoredAuthSession
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
    private var authSession: StoredAuthSession? = null
    private var biometricChoice = BiometricUnlockChoice.UNDECIDED
    private var biometricBlob: CipherBlob? = null
    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) { stored = secrets }
    override suspend fun loadVaultSecrets(): StoredVaultSecrets? = stored
    override suspend fun clear() { stored = null }
    override suspend fun saveAuthSession(session: StoredAuthSession) { authSession = session }
    override suspend fun loadAuthSession(): StoredAuthSession? = authSession
    override suspend fun clearAuthSession() { authSession = null }
    override suspend fun saveBiometricChoice(choice: BiometricUnlockChoice) { biometricChoice = choice }
    override suspend fun loadBiometricChoice(): BiometricUnlockChoice = biometricChoice
    override suspend fun saveBiometricKeystoreBlob(blob: CipherBlob) { biometricBlob = blob }
    override suspend fun loadBiometricKeystoreBlob(): CipherBlob? = biometricBlob
    override suspend fun clearBiometricKeystoreBlob() { biometricBlob = null }
}

class VaultRepositoryImplTest {

    private lateinit var database: OrionVaultDatabase

    /**
     * Cada chamada cria um banco novo (e reaponta [database] para ele), então dá para simular dois
     * dispositivos independentes instanciando dois repositórios e guardando cada `database` numa
     * variável local logo após a criação.
     */
    private fun newRepository(
        secureStore: SecureCredentialStore = InMemorySecureCredentialStore()
    ): VaultRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        OrionVaultDatabase.Schema.create(driver)
        database = OrionVaultDatabase(driver)
        // accountSessionManager nulo = "nunca logado na nuvem": tombstones são purgados na hora.
        return VaultRepositoryImpl(database, secureStore)
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

    /**
     * O caso que motivou `restoreVault`: um segundo dispositivo precisa derivar exatamente a mesma
     * chave para conseguir decifrar o que o sync baixar. Aqui o "download" é simulado copiando a
     * linha cifrada do banco A para o banco B — nada é recifrado no caminho, igual ao sync real.
     */
    @Test
    fun restoreVaultDerivesTheSameKeyAndDecryptsItemsFromTheOtherDevice() = runTest {
        ensureLibsodiumInitialized()
        val storeA = InMemorySecureCredentialStore()
        val deviceA = newRepository(storeA)
        val databaseA = database

        val secretKeyDisplay = deviceA.createVault("correct horse".toCharArray())
        val folder = deviceA.createFolder(null, "Trabalho")
        val item = deviceA.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        val params = deviceA.getLocalKdfParams()!!
        val itemRow = databaseA.vaultQueries.selectItemById(item.id).executeAsOne()

        val deviceB = newRepository(InMemorySecureCredentialStore())
        val databaseB = database
        deviceB.restoreVault(
            "correct horse".toCharArray(),
            SecretKeyGenerator.parseFromDisplay(secretKeyDisplay),
            params
        )
        assertTrue(deviceB.isVaultInitialized())

        databaseB.vaultQueries.upsertRemoteFolder(
            folder.id, null, folder.name, folder.createdAt, folder.updatedAt, 1L, 1L
        )
        databaseB.vaultQueries.upsertRemoteItem(
            itemRow.id, itemRow.folderId, itemRow.title, itemRow.usernameCipher, itemRow.emailCipher,
            itemRow.passwordCipher, itemRow.urlCipher, itemRow.notesCipher, itemRow.createdAt,
            itemRow.updatedAt, 1L, 1L
        )

        val restored = deviceB.listItems(folder.id).single()
        assertEquals("Gmail", restored.title)
        assertEquals("s3cr3t", restored.password)

        // E o canary local passa a valer para a mesma Master Password.
        deviceB.lock()
        assertTrue(deviceB.unlock("correct horse".toCharArray()))
    }

    @Test
    fun restoreVaultWithWrongSecretKeyDerivesADifferentKey() = runTest {
        ensureLibsodiumInitialized()
        val deviceA = newRepository()
        val databaseA = database
        deviceA.createVault("correct horse".toCharArray())
        val params = deviceA.getLocalKdfParams()!!
        val item = deviceA.createItem(null, "Gmail", null, null, "s3cr3t", null, null)
        val itemRow = databaseA.vaultQueries.selectItemById(item.id).executeAsOne()

        val deviceB = newRepository(InMemorySecureCredentialStore())
        val databaseB = database
        deviceB.restoreVault(
            "correct horse".toCharArray(),
            SecretKeyGenerator.parseFromDisplay("00000000-00000000-00000000-00000000"),
            params
        )
        databaseB.vaultQueries.upsertRemoteItem(
            itemRow.id, null, itemRow.title, itemRow.usernameCipher, itemRow.emailCipher,
            itemRow.passwordCipher, itemRow.urlCipher, itemRow.notesCipher, itemRow.createdAt,
            itemRow.updatedAt, 1L, 1L
        )

        // O cofre local fica coerente consigo mesmo (o canary foi criado com a chave errada, então
        // o unlock passa), mas a chave é outra: o que veio do dispositivo A não decifra e a
        // listagem pula o item em vez de quebrar. Detectar isso exigiria ciphertext do servidor
        // em mãos no momento da restauração — hoje a UI só pode alertar o usuário.
        assertTrue(deviceB.unlock("correct horse".toCharArray()))
        assertTrue(deviceB.listItems(null).isEmpty())
    }

    @Test
    fun verifyKeyAgainstCiphertextAcceptsTheCorrectSecretKey() = runTest {
        ensureLibsodiumInitialized()
        val deviceA = newRepository()
        val databaseA = database
        val secretKeyDisplay = deviceA.createVault("correct horse".toCharArray())
        val params = deviceA.getLocalKdfParams()!!
        val item = deviceA.createItem(null, "Gmail", null, null, "s3cr3t", null, null)
        val sampleCipher = databaseA.vaultQueries.selectItemById(item.id).executeAsOne().passwordCipher

        val deviceB = newRepository(InMemorySecureCredentialStore())
        assertTrue(
            deviceB.verifyKeyAgainstCiphertext(
                "correct horse".toCharArray(),
                SecretKeyGenerator.parseFromDisplay(secretKeyDisplay),
                params,
                sampleCipher
            )
        )
    }

    @Test
    fun verifyKeyAgainstCiphertextRejectsAWrongSecretKeyOrPassword() = runTest {
        ensureLibsodiumInitialized()
        val deviceA = newRepository()
        val databaseA = database
        val secretKeyDisplay = deviceA.createVault("correct horse".toCharArray())
        val params = deviceA.getLocalKdfParams()!!
        val item = deviceA.createItem(null, "Gmail", null, null, "s3cr3t", null, null)
        val sampleCipher = databaseA.vaultQueries.selectItemById(item.id).executeAsOne().passwordCipher

        val deviceB = newRepository(InMemorySecureCredentialStore())
        assertFalse(
            deviceB.verifyKeyAgainstCiphertext(
                "correct horse".toCharArray(),
                SecretKeyGenerator.parseFromDisplay("00000000-00000000-00000000-00000000"),
                params,
                sampleCipher
            )
        )
        assertFalse(
            deviceB.verifyKeyAgainstCiphertext(
                "wrong horse".toCharArray(),
                SecretKeyGenerator.parseFromDisplay(secretKeyDisplay),
                params,
                sampleCipher
            )
        )
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
        val item = repo.createItem(folder.id, "Gmail", "user@example.com", "user@email.com", "s3cr3t", "https://gmail.com", "nota")

        val items = repo.listItems(folder.id)
        assertEquals(1, items.size)
        assertEquals("Gmail", items[0].title)
        assertEquals("user@example.com", items[0].username)
        assertEquals("user@email.com", items[0].email)
        assertEquals("s3cr3t", items[0].password)
        assertEquals("https://gmail.com", items[0].url)
        assertEquals("nota", items[0].notes)
        assertEquals(1, items[0].version)
        assertEquals(item.id, items[0].id)
    }

    @Test
    fun createItemWithoutEmailLeavesEmailNull() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")

        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        assertNull(item.email)
        assertNull(repo.listItems(folder.id).single().email)
    }

    @Test
    fun updateItemCanAddAndRemoveEmail() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        repo.updateItem(item.id, folder.id, "Gmail", null, "added@email.com", "s3cr3t", null, null)
        assertEquals("added@email.com", repo.listItems(folder.id).single().email)

        repo.updateItem(item.id, folder.id, "Gmail", null, null, "s3cr3t", null, null)
        assertNull(repo.listItems(folder.id).single().email)
    }

    @Test
    fun updateItemIncrementsVersionAndChangesCipher() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        repo.updateItem(item.id, folder.id, "Gmail", null, null, "n0v4s3nh4", null, null)

        val updated = repo.listItems(folder.id).single()
        assertEquals(2, updated.version)
        assertEquals("n0v4s3nh4", updated.password)
    }

    @Test
    fun updateItemCanMoveItemBetweenFolders() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folderA = repo.createFolder(null, "Trabalho")
        val folderB = repo.createFolder(null, "Pessoal")
        val item = repo.createItem(folderA.id, "Gmail", null, null, "s3cr3t", null, null)

        repo.updateItem(item.id, folderB.id, "Gmail", null, null, "s3cr3t", null, null)

        assertTrue(repo.listItems(folderA.id).isEmpty())
        assertEquals(item.id, repo.listItems(folderB.id).single().id)
    }

    @Test
    fun updateItemCanUnfileItemFromFolder() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        repo.updateItem(item.id, null, "Gmail", null, null, "s3cr3t", null, null)

        assertTrue(repo.listItems(folder.id).isEmpty())
        assertEquals(item.id, repo.listItems(null).single().id)
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
    fun newRowsStartDirtyNeverPushed() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())

        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        assertEquals(SyncState.DIRTY_NEW, database.vaultQueries.selectFolderById(folder.id).executeAsOne().syncState)
        assertEquals(SyncState.DIRTY_NEW, database.vaultQueries.selectItemById(item.id).executeAsOne().syncState)
    }

    @Test
    fun editingACleanRowMarksItDirtyUpdated() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        repo.markFolderSynced(folder.id, 1L)
        repo.markItemSynced(item.id, 1L)
        repo.renameFolder(folder.id, "Trabalho 2")
        repo.updateItem(item.id, folder.id, "Gmail", null, null, "outra", null, null)

        val folderRow = database.vaultQueries.selectFolderById(folder.id).executeAsOne()
        val itemRow = database.vaultQueries.selectItemById(item.id).executeAsOne()
        assertEquals(SyncState.DIRTY_UPDATED, folderRow.syncState)
        assertEquals(SyncState.DIRTY_UPDATED, itemRow.syncState)
        // syncedVersion continua sendo a versão confirmada pelo servidor (base otimista),
        // enquanto `version` é o contador de revisões locais.
        assertEquals(1L, folderRow.syncedVersion)
        assertEquals(2L, folderRow.version)
        assertEquals(1L, itemRow.syncedVersion)
        assertEquals(2L, itemRow.version)
    }

    @Test
    fun deleteRemovesRowEntirelyWhenNeverSynced() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())
        val folder = repo.createFolder(null, "Trabalho")
        val item = repo.createItem(folder.id, "Gmail", null, null, "s3cr3t", null, null)

        repo.deleteItem(item.id)
        repo.deleteFolder(folder.id)

        // Sem conta na nuvem (ou linha nunca enviada) não faz sentido guardar tombstone.
        assertNull(database.vaultQueries.selectItemById(item.id).executeAsOneOrNull())
        assertNull(database.vaultQueries.selectFolderById(folder.id).executeAsOneOrNull())
        assertTrue(repo.listAllFolders().isEmpty())
    }

    /**
     * Antes da correção, `deleteFolder` só tombstoneava a própria pasta: subpastas e itens dentro
     * dela ficavam órfãos localmente (invisíveis na listagem) mas nunca eram marcados como sujos/
     * tombstone, então nunca eram enviados ao servidor — o item "ressuscitava" na nuvem para sempre.
     */
    @Test
    fun deleteFolderCascadesToDescendantSubfoldersAndItems() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())

        val root = repo.createFolder(null, "Trabalho")
        val child = repo.createFolder(root.id, "Projetos")
        val rootItem = repo.createItem(root.id, "Gmail", null, null, "s3cr3t", null, null)
        val childItem = repo.createItem(child.id, "AWS", null, null, "s3cr3t", null, null)

        repo.deleteFolder(root.id)

        assertNull(database.vaultQueries.selectFolderById(root.id).executeAsOneOrNull())
        assertNull(database.vaultQueries.selectFolderById(child.id).executeAsOneOrNull())
        assertNull(database.vaultQueries.selectItemById(rootItem.id).executeAsOneOrNull())
        assertNull(database.vaultQueries.selectItemById(childItem.id).executeAsOneOrNull())
        assertTrue(repo.listAllFolders().isEmpty())
    }

    @Test
    fun syncCursorAndDeviceIdRoundTrip() = runTest {
        ensureLibsodiumInitialized()
        val repo = newRepository()
        repo.createVault("correct horse".toCharArray())

        assertNull(repo.getSyncCursor())
        repo.setSyncCursor(1234L)
        assertEquals(1234L, repo.getSyncCursor())

        val deviceId = repo.getOrCreateDeviceId()
        assertTrue(deviceId.isNotBlank())
        assertEquals(deviceId, repo.getOrCreateDeviceId())
        // Escrever o deviceId não pode apagar o cursor (e vice-versa).
        assertEquals(1234L, repo.getSyncCursor())
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
