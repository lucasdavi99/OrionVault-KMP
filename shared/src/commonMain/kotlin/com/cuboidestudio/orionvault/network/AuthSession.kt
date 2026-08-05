package com.cuboidestudio.orionvault.network

/**
 * Sessão de conta na nuvem (Supabase Auth). Nada aqui tem relação com a criptografia do cofre:
 * a Master Password, a Secret Key e a chave derivada do cofre nunca chegam perto desta classe.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMillis: Long,
    val userId: String,
    val email: String
)

/**
 * Resultado de um `signUp`: o Supabase devolve uma sessão completa quando o auto-confirm está
 * ligado, e nenhuma sessão quando "Confirm email" está ativo (o usuário precisa clicar no link
 * do e-mail antes de conseguir logar). Os dois casos são sucesso, não falha.
 */
sealed interface SignUpResult {
    data class SignedIn(val session: AuthSession) : SignUpResult
    data object ConfirmationRequired : SignUpResult
}
