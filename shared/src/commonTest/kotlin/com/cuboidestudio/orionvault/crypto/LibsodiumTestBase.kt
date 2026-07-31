package com.cuboidestudio.orionvault.crypto

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

/** Garante que o libsodium foi inicializado antes de cada teste de criptografia. */
abstract class LibsodiumTestBase {
    fun runLibsodiumTest(block: suspend () -> Unit): TestResult = runTest {
        ensureLibsodiumInitialized()
        block()
    }
}
