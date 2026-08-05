package com.cuboidestudio.orionvault.storage.secure

import android.content.Context

/**
 * TODO: placeholder usando `SharedPreferences` comum — migrar para Android Keystore
 * (ex.: `EncryptedSharedPreferences` ou Tink) antes de qualquer uso em produção
 * (ver seção 11, item 7 e seção 12.1 do design doc).
 */
class AndroidSecureCredentialStore(private val context: Context) : SecureCredentialStore {
    private val prefs by lazy { context.getSharedPreferences("orionvault_secure", Context.MODE_PRIVATE) }

    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) {
        prefs.edit().putString(KEY, VaultSecretsSerializer.serialize(secrets)).apply()
    }

    override suspend fun loadVaultSecrets(): StoredVaultSecrets? {
        val raw = prefs.getString(KEY, null) ?: return null
        return VaultSecretsSerializer.deserialize(raw)
    }

    override suspend fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    override suspend fun saveAuthSession(session: StoredAuthSession) {
        prefs.edit().putString(AUTH_KEY, AuthSessionSerializer.serialize(session)).apply()
    }

    override suspend fun loadAuthSession(): StoredAuthSession? {
        val raw = prefs.getString(AUTH_KEY, null) ?: return null
        return AuthSessionSerializer.deserialize(raw)
    }

    override suspend fun clearAuthSession() {
        prefs.edit().remove(AUTH_KEY).apply()
    }

    companion object {
        private const val KEY = "vault_secrets"
        private const val AUTH_KEY = "cloud_auth_session"
    }
}
