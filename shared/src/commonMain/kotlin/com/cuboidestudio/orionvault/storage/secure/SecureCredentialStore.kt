package com.cuboidestudio.orionvault.storage.secure

import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.cuboidestudio.orionvault.crypto.KdfParams

/**
 * Segredos do cofre que precisam sobreviver ao fechamento do app, mas nunca podem
 * ser expostos ao backend: a Secret Key (design doc seção 3.1), os parâmetros do KDF
 * usados para derivar a chave, e o canary usado para validar a Master Password no unlock.
 */
class StoredVaultSecrets(
    val secretKey: ByteArray,
    val kdfParams: KdfParams,
    val canary: CipherBlob
)

interface SecureCredentialStore {
    suspend fun saveVaultSecrets(secrets: StoredVaultSecrets)
    suspend fun loadVaultSecrets(): StoredVaultSecrets?
    suspend fun clear()
}

/** Contexto opaco necessário por algumas plataformas (ex.: `Context` no Android) para acessar armazenamento seguro. */
expect class PlatformContext

expect fun createSecureCredentialStore(context: PlatformContext): SecureCredentialStore
