package com.cuboidestudio.orionvault.domain.model

data class VaultFolder(
    val id: String,
    val parentId: String?,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Contador de revisões locais, espelhando [VaultItem.version]. */
    val version: Int = 1
)
