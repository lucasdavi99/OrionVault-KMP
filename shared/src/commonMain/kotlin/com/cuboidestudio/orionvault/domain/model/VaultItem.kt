package com.cuboidestudio.orionvault.domain.model

/** Item do cofre já decriptado — só deve existir em memória depois do unlock (zero-knowledge). */
data class VaultItem(
    val id: String,
    val folderId: String?,
    val title: String,
    val username: String?,
    val email: String?,
    val password: String,
    val url: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Int
)
