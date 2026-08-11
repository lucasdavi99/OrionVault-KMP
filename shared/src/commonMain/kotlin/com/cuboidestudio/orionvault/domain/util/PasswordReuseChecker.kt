package com.cuboidestudio.orionvault.domain.util

/**
 * Detecção de reuso de senha dentro do cofre. Função pura: quem chama já é responsável por
 * excluir a própria senha do item sendo editado da lista comparada.
 */
internal object PasswordReuseChecker {
    fun countUsages(password: String, allPasswords: List<String>): Int {
        if (password.isEmpty()) return 0
        return allPasswords.count { it == password }
    }
}
