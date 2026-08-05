package com.cuboidestudio.orionvault.viewmodel

sealed interface Route {
    data object Loading : Route
    data object Onboarding : Route
    data object Unlock : Route
    data object Vault : Route
    data class EditItem(val folderId: String?, val itemId: String?) : Route
    data class EditFolder(val parentId: String?, val folderId: String?) : Route

    /** Tela combinada de login/cadastro da conta na nuvem. Fora do fluxo obrigatório Onboarding->Unlock. */
    data object AccountAuth : Route

    /** Status do sync, botão de sincronizar agora, logout e lista de conflitos. */
    data object SyncSettings : Route
}
