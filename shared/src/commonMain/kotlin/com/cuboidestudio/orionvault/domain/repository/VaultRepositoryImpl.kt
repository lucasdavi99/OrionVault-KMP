@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.cuboidestudio.orionvault.domain.repository

import com.cuboidestudio.orionvault.crypto.AeadCipher
import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.cuboidestudio.orionvault.crypto.KdfParamsV1
import com.cuboidestudio.orionvault.crypto.KeyDerivation
import com.cuboidestudio.orionvault.crypto.SecretKeyGenerator
import com.cuboidestudio.orionvault.crypto.VaultCanary
import com.cuboidestudio.orionvault.crypto.ensureLibsodiumInitialized
import com.cuboidestudio.orionvault.domain.model.VaultConstants
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import com.cuboidestudio.orionvault.storage.db.OrionVaultDatabase
import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
import com.cuboidestudio.orionvault.storage.secure.StoredVaultSecrets
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class VaultRepositoryImpl(
    private val database: OrionVaultDatabase,
    private val secureStore: SecureCredentialStore
) : VaultRepository {
    private var sessionKey: ByteArray? = null

    override suspend fun isVaultInitialized(): Boolean = secureStore.loadVaultSecrets() != null

    override suspend fun createVault(masterPassword: CharArray): String {
        ensureLibsodiumInitialized()
        val secretKey = SecretKeyGenerator.generate()
        val params = KdfParamsV1.newParams()
        val key = KeyDerivation.deriveVaultKey(masterPassword, secretKey, params)
        val canary = VaultCanary.create(key)
        secureStore.saveVaultSecrets(StoredVaultSecrets(secretKey, params, canary))
        sessionKey = key
        return SecretKeyGenerator.formatForDisplay(secretKey)
    }

    override suspend fun unlock(masterPassword: CharArray): Boolean {
        ensureLibsodiumInitialized()
        val stored = secureStore.loadVaultSecrets() ?: return false
        val key = KeyDerivation.deriveVaultKey(masterPassword, stored.secretKey, stored.kdfParams)
        if (!VaultCanary.verify(stored.canary, key)) return false
        sessionKey = key
        return true
    }

    override fun lock() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    override fun isUnlocked(): Boolean = sessionKey != null

    private fun requireKey(): ByteArray = sessionKey ?: error("Vault está bloqueado")

    override suspend fun listFolders(parentId: String?): List<VaultFolder> =
        database.vaultQueries.selectFoldersByParent(parentId).executeAsList().map {
            VaultFolder(it.id, it.parentId, it.name, it.createdAt, it.updatedAt)
        }

    override suspend fun createFolder(parentId: String?, name: String): VaultFolder {
        val newDepth = depthOf(parentId) + 1
        if (newDepth > VaultConstants.MAX_FOLDER_DEPTH) {
            throw FolderDepthExceededException(
                "Profundidade máxima de pastas (${VaultConstants.MAX_FOLDER_DEPTH}) excedida"
            )
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val id = newId()
        database.vaultQueries.insertFolder(id, parentId, name, now, now)
        return VaultFolder(id, parentId, name, now, now)
    }

    override suspend fun renameFolder(id: String, newName: String) {
        database.vaultQueries.renameFolder(newName, Clock.System.now().toEpochMilliseconds(), id)
    }

    override suspend fun deleteFolder(id: String) {
        database.vaultQueries.deleteFolder(id)
    }

    override suspend fun listItems(folderId: String): List<VaultItem> {
        ensureLibsodiumInitialized()
        val key = requireKey()
        return database.vaultQueries.selectItemsByFolder(folderId).executeAsList().map { row ->
            VaultItem(
                id = row.id,
                folderId = row.folderId,
                title = row.title,
                username = row.usernameCipher?.let { decryptField(it, key) },
                password = decryptField(row.passwordCipher, key),
                url = row.urlCipher?.let { decryptField(it, key) },
                notes = row.notesCipher?.let { decryptField(it, key) },
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                version = row.version.toInt()
            )
        }
    }

    override suspend fun createItem(
        folderId: String,
        title: String,
        username: String?,
        password: String,
        url: String?,
        notes: String?
    ): VaultItem {
        ensureLibsodiumInitialized()
        val key = requireKey()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = newId()
        database.vaultQueries.insertItem(
            id, folderId, title,
            username?.let { encryptField(it, key) },
            encryptField(password, key),
            url?.let { encryptField(it, key) },
            notes?.let { encryptField(it, key) },
            now, now, 1L
        )
        return VaultItem(id, folderId, title, username, password, url, notes, now, now, 1)
    }

    override suspend fun updateItem(
        id: String,
        title: String,
        username: String?,
        password: String,
        url: String?,
        notes: String?
    ) {
        ensureLibsodiumInitialized()
        val key = requireKey()
        val current = database.vaultQueries.selectItemById(id).executeAsOne()
        val now = Clock.System.now().toEpochMilliseconds()
        database.vaultQueries.updateItem(
            title,
            username?.let { encryptField(it, key) },
            encryptField(password, key),
            url?.let { encryptField(it, key) },
            notes?.let { encryptField(it, key) },
            now,
            current.version + 1,
            id
        )
    }

    override suspend fun deleteItem(id: String) {
        database.vaultQueries.deleteItem(id)
    }

    private fun depthOf(folderId: String?): Int {
        var depth = 0
        var current = folderId
        while (current != null) {
            depth++
            current = database.vaultQueries.selectFolderById(current).executeAsOneOrNull()?.parentId
        }
        return depth
    }

    private fun newId(): String = Uuid.random().toString()

    private fun encryptField(plaintext: String, key: ByteArray): String =
        AeadCipher.encrypt(plaintext.encodeToByteArray(), key).toStorageString()

    private fun decryptField(stored: String, key: ByteArray): String =
        AeadCipher.decrypt(CipherBlob.fromStorageString(stored), key).decodeToString()
}
