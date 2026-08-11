package com.cuboidestudio.orionvault.domain.util

/**
 * SHA-1 puro em Kotlin, sem dependência externa nem cinterop por plataforma. Usado só para a
 * consulta k-anonymity ao Have I Been Pwned (recurso consultivo) — a criptografia real do cofre
 * continua inteiramente em libsodium, ver `crypto/`.
 */
internal object Sha1 {
    private val K = intArrayOf(0x5A827999, 0x6ED9EBA1, -0x70E44324, -0x359D3E2A) // 0x8F1BBCDC, 0xCA62C1D6

    fun hash(input: ByteArray): ByteArray {
        var h0 = 0x67452301
        var h1 = -0x10325477 // 0xEFCDAB89
        var h2 = -0x67452302 // 0x98BADCFE
        var h3 = 0x10325476
        var h4 = -0x3C2D1E10 // 0xC3D2E1F0

        val messageBitLength = input.size.toLong() * 8
        val paddingZeroBytes = ((55 - input.size % 64) + 64) % 64
        val padded = ByteArray(input.size + 1 + paddingZeroBytes + 8)
        input.copyInto(padded)
        padded[input.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[padded.size - 1 - i] = ((messageBitLength ushr (8 * i)) and 0xFF).toByte()
        }

        val w = IntArray(80)
        var chunkStart = 0
        while (chunkStart < padded.size) {
            for (i in 0 until 16) {
                val offset = chunkStart + i * 4
                w[i] = ((padded[offset].toInt() and 0xFF) shl 24) or
                    ((padded[offset + 1].toInt() and 0xFF) shl 16) or
                    ((padded[offset + 2].toInt() and 0xFF) shl 8) or
                    (padded[offset + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    i < 20 -> {
                        f = (b and c) or (b.inv() and d)
                        k = K[0]
                    }
                    i < 40 -> {
                        f = b xor c xor d
                        k = K[1]
                    }
                    i < 60 -> {
                        f = (b and c) or (b and d) or (c and d)
                        k = K[2]
                    }
                    else -> {
                        f = b xor c xor d
                        k = K[3]
                    }
                }
                val temp = a.rotateLeft(5) + f + e + k + w[i]
                e = d
                d = c
                c = b.rotateLeft(30)
                b = a
                a = temp
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e

            chunkStart += 64
        }

        val digest = ByteArray(20)
        intArrayOf(h0, h1, h2, h3, h4).forEachIndexed { index, value ->
            digest[index * 4] = (value ushr 24).toByte()
            digest[index * 4 + 1] = (value ushr 16).toByte()
            digest[index * 4 + 2] = (value ushr 8).toByte()
            digest[index * 4 + 3] = value.toByte()
        }
        return digest
    }

    /** Hex maiúsculo de 40 caracteres — mesma convenção usada na documentação da API do HIBP. */
    fun hex(input: String): String =
        hash(input.encodeToByteArray()).joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }.uppercase()
}
