package com.cuboidestudio.orionvault.storage.secure

import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.cuboidestudio.orionvault.crypto.KdfParams
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Serialização simples em texto (uma linha por campo) usada pelas implementações de
 * [SecureCredentialStore] para persistir [StoredVaultSecrets]. Não é um formato público,
 * só precisa ser lido pelo próprio app.
 */
@OptIn(ExperimentalEncodingApi::class)
object VaultSecretsSerializer {
    private const val FORMAT_VERSION = 1

    fun serialize(secrets: StoredVaultSecrets): String = buildString {
        appendLine(FORMAT_VERSION)
        appendLine(secrets.kdfParams.version)
        appendLine(secrets.kdfParams.opsLimit)
        appendLine(secrets.kdfParams.memLimitBytes)
        appendLine(Base64.encode(secrets.kdfParams.salt))
        appendLine(Base64.encode(secrets.secretKey))
        append(secrets.canary.toStorageString())
    }

    fun deserialize(raw: String): StoredVaultSecrets {
        val lines = raw.split("\n")
        require(lines.size >= 7) { "Formato de segredos do cofre inválido" }
        require(lines[0].trim().toInt() == FORMAT_VERSION) { "Versão de formato de segredos desconhecida" }
        val kdfParams = KdfParams(
            version = lines[1].trim().toInt(),
            opsLimit = lines[2].trim().toULong(),
            memLimitBytes = lines[3].trim().toInt(),
            salt = Base64.decode(lines[4].trim())
        )
        val secretKey = Base64.decode(lines[5].trim())
        val canary = CipherBlob.fromStorageString(lines[6].trim())
        return StoredVaultSecrets(secretKey, kdfParams, canary)
    }
}
