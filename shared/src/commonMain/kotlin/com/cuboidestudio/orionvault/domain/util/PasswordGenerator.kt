package com.cuboidestudio.orionvault.domain.util

import kotlin.random.Random

object PasswordGenerator {
    private const val CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#\$%^&*-_=+"

    fun generate(length: Int = 20): String =
        (1..length).map { CHARSET[Random.nextInt(CHARSET.length)] }.joinToString("")

    fun strength(password: String): Pair<Float, String> {
        if (password.isEmpty()) return 0f to "—"
        var variety = 0
        if (password.any { it.isLowerCase() }) variety++
        if (password.any { it.isUpperCase() }) variety++
        if (password.any { it.isDigit() }) variety++
        if (password.any { !it.isLetterOrDigit() }) variety++
        val lengthScore = (password.length.coerceAtMost(24) / 24f)
        val score = (lengthScore * 0.6f + (variety / 4f) * 0.4f).coerceIn(0f, 1f)
        val label = when {
            score < 0.35f -> "Fraca"
            score < 0.6f -> "Razoável"
            score < 0.85f -> "Forte"
            else -> "Excelente"
        }
        return score to label
    }
}
