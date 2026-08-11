package com.cuboidestudio.orionvault.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PasswordReuseCheckerTest {
    @Test
    fun `empty list returns zero`() {
        assertEquals(0, PasswordReuseChecker.countUsages("abc123", emptyList()))
    }

    @Test
    fun `one duplicate returns one`() {
        assertEquals(1, PasswordReuseChecker.countUsages("abc123", listOf("abc123", "xyz789")))
    }

    @Test
    fun `comparison is case sensitive`() {
        assertEquals(0, PasswordReuseChecker.countUsages("Password1", listOf("password1")))
    }

    @Test
    fun `counts multiple duplicates`() {
        val others = listOf("abc123", "abc123", "abc123", "xyz789")
        assertEquals(3, PasswordReuseChecker.countUsages("abc123", others))
    }
}
