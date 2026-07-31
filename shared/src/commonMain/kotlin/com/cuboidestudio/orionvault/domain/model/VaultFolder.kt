package com.cuboidestudio.orionvault.domain.model

data class VaultFolder(
    val id: String,
    val parentId: String?,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)
