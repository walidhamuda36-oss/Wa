package com.example.data.model

data class DecryptedVaultItem(
    val id: Long = 0,
    val title: String,
    val categoryId: String,
    val itemType: String,
    val fields: List<VaultField>,
    val notes: String = "",
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val isSensitive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
