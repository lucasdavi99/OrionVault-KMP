package com.cuboidestudio.orionvault.domain.util

/**
 * Estado da checagem de vazamento (Have I Been Pwned) para a senha em edição. [CheckFailed] não
 * deve virar nenhum aviso na UI — falha de rede é sempre silenciosa e nunca bloqueia o "Salvar".
 */
sealed interface BreachStatus {
    data object Idle : BreachStatus
    data object Checking : BreachStatus
    data class Breached(val count: Int) : BreachStatus
    data object NotBreached : BreachStatus
    data object CheckFailed : BreachStatus
}
