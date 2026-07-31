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

    companion object {
        private const val KEY = "vault_secrets"
    }
}
