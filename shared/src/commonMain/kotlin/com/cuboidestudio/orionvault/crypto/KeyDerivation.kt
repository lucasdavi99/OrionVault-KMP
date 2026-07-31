@file:OptIn(ExperimentalUnsignedTypes::class)

package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_ALG_DEFAULT
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Deriva a chave de criptografia do cofre a partir da combinação de Master Password + Secret Key
 * (design doc seção 3.1) via Argon2id. Nenhuma delas isoladamente é suficiente para derivar a chave;
 * a Secret Key funciona como um "pepper" de alta entropia que nunca sai do dispositivo.
 *
 * Limitação conhecida: a API de pwhash da libsodium-bindings recebe a senha como String, então a
 * senha combinada não pode ser zerada da memória após o uso (limitação da biblioteca, não deste código).
 */
object KeyDerivation {
    @OptIn(ExperimentalEncodingApi::class)
    fun deriveVaultKey(masterPassword: CharArray, secretKey: ByteArray, params: KdfParams): ByteArray {
        val combined = buildString {
            append(masterPassword)
            append(':')
            append(Base64.encode(secretKey))
        }
        val derived = PasswordHash.pwhash(
            outputLength = KdfParamsV1.OUTPUT_LENGTH_BYTES,
            password = combined,
            salt = params.salt.asUByteArray(),
            opsLimit = params.opsLimit,
            memLimit = params.memLimitBytes,
            algorithm = crypto_pwhash_ALG_DEFAULT
        )
        return derived.asByteArray()
    }
}
