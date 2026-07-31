@file:OptIn(ExperimentalUnsignedTypes::class)

package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_MEMLIMIT_MODERATE
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_OPSLIMIT_MODERATE
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_SALTBYTES
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

/**
 * Parâmetros do Argon2id usados para derivar a chave do cofre, versionados para que
 * mudanças futuras de custo não invalidem cofres já existentes (design doc seção 3.2).
 */
class KdfParams(
    val version: Int,
    val opsLimit: ULong,
    val memLimitBytes: Int,
    val salt: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdfParams) return false
        return version == other.version &&
            opsLimit == other.opsLimit &&
            memLimitBytes == other.memLimitBytes &&
            salt.contentEquals(other.salt)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + opsLimit.hashCode()
        result = 31 * result + memLimitBytes
        result = 31 * result + salt.contentHashCode()
        return result
    }
}

/**
 * Parâmetros versão 1. Usa os limites "moderate" do libsodium (256 MiB, 3 iterações) como
 * baseline de custo — mais forte que "interactive", adequado para proteger a master password
 * de um cofre local. Revisar/benchmarcar em dispositivo-alvo mínimo antes de produção
 * (ponto em aberto na seção 11 do design doc).
 */
object KdfParamsV1 {
    const val VERSION = 1
    val OPS_LIMIT: ULong = crypto_pwhash_OPSLIMIT_MODERATE
    const val MEM_LIMIT_BYTES: Int = crypto_pwhash_MEMLIMIT_MODERATE
    const val OUTPUT_LENGTH_BYTES = 32
    const val SALT_BYTES = crypto_pwhash_SALTBYTES

    fun generateSalt(): ByteArray = LibsodiumRandom.buf(SALT_BYTES).asByteArray()

    fun newParams(): KdfParams = KdfParams(VERSION, OPS_LIMIT, MEM_LIMIT_BYTES, generateSalt())
}
