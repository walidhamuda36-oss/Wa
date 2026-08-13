package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VaultField(
    val label: String,
    val value: String,
    val isMasked: Boolean = false,
    val fieldType: String = "TEXT" // TEXT, PHONE, ID, NUMBER, DATE, NOTE
)

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val categoryId: String,
    val itemType: String, // PROFILE, ID_NUMBERS, PHONE_CONTACT, DOCUMENT, NOTE, FINANCIAL, CUSTOM
    val encryptedFieldsJson: String, // Encrypted JSON string of List<VaultField>
    val encryptedNotes: String = "", // Encrypted note body string
    val photoUri: String? = null, // URI string for profile picture or document attachment
    val isFavorite: Boolean = false,
    val isSensitive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
