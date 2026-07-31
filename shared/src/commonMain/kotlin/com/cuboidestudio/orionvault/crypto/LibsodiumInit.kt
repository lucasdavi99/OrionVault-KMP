package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer

/**
 * Garante que o libsodium foi inicializado antes de qualquer operação de criptografia.
 * Idempotente — chamadas repetidas depois da primeira inicialização são no-op.
 */
suspend fun ensureLibsodiumInitialized() {
    if (!LibsodiumInitializer.isInitialized()) {
        LibsodiumInitializer.initialize()
    }
}
