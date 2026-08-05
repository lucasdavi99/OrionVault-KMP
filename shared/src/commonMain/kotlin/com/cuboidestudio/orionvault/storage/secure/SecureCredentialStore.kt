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

/**
 * Sessão da conta na nuvem persistida entre execuções (tokens do Supabase Auth).
 *
 * Totalmente independente de [StoredVaultSecrets]: perder/limpar esta sessão nunca afeta o
 * cofre local, e o cofre local nunca depende dela para abrir.
 *
 * GAP CONHECIDO: nas implementações Android (SharedPreferences) e iOS (NSUserDefaults) — que
 * já eram TODOs de migração para Keystore/Keychain — o refresh token herda a mesma proteção
 * fraca em repouso dos segredos do cofre. Não é regressão, mas amplia a superfície: um refresh
 * token vazado dá acesso ao *ciphertext* na nuvem (nunca ao texto claro, que exige a Master
 * Password + Secret Key). Endereçar junto com os TODOs originais.
 */
class StoredAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val userId: String,
    val email: String
)

interface SecureCredentialStore {
    suspend fun saveVaultSecrets(secrets: StoredVaultSecrets)
    suspend fun loadVaultSecrets(): StoredVaultSecrets?
    suspend fun clear()

    suspend fun saveAuthSession(session: StoredAuthSession)
    suspend fun loadAuthSession(): StoredAuthSession?
    suspend fun clearAuthSession()
}

/** Contexto opaco necessário por algumas plataformas (ex.: `Context` no Android) para acessar armazenamento seguro. */
expect class PlatformContext

expect fun createSecureCredentialStore(context: PlatformContext): SecureCredentialStore
