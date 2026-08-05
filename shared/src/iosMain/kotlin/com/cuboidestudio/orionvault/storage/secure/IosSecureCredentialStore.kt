package com.cuboidestudio.orionvault.storage.secure

import platform.Foundation.NSUserDefaults

/**
 * TODO: placeholder usando `NSUserDefaults` — migrar para Keychain antes de qualquer uso
 * em produção (ver seção 11, item 7 e seção 12.1 do design doc).
 */
class IosSecureCredentialStore : SecureCredentialStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) {
        defaults.setObject(VaultSecretsSerializer.serialize(secrets), KEY)
    }

    override suspend fun loadVaultSecrets(): StoredVaultSecrets? {
        val raw = defaults.stringForKey(KEY) ?: return null
        return VaultSecretsSerializer.deserialize(raw)
    }

    override suspend fun clear() {
        defaults.removeObjectForKey(KEY)
    }

    override suspend fun saveAuthSession(session: StoredAuthSession) {
        defaults.setObject(AuthSessionSerializer.serialize(session), AUTH_KEY)
    }

    override suspend fun loadAuthSession(): StoredAuthSession? {
        val raw = defaults.stringForKey(AUTH_KEY) ?: return null
        return AuthSessionSerializer.deserialize(raw)
    }

    override suspend fun clearAuthSession() {
        defaults.removeObjectForKey(AUTH_KEY)
    }

    companion object {
        private const val KEY = "vault_secrets"
        private const val AUTH_KEY = "cloud_auth_session"
    }
}
