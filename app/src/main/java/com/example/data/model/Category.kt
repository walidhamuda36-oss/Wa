package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(id = "cat_profile", name = "Profile & General", iconName = "Person", colorHex = "#3B82F6", sortOrder = 1),
            Category(id = "cat_ids", name = "ID & Passports", iconName = "Badge", colorHex = "#6366F1", sortOrder = 2),
            Category(id = "cat_contacts", name = "Phones & Contacts", iconName = "Phone", colorHex = "#10B981", sortOrder = 3),
            Category(id = "cat_docs", name = "Documents & Files", iconName = "Description", colorHex = "#8B5CF6", sortOrder = 4),
            Category(id = "cat_financial", name = "Cards & Finance", iconName = "CreditCard", colorHex = "#F59E0B", sortOrder = 5),
            Category(id = "cat_medical", name = "Medical & Health", iconName = "MedicalServices", colorHex = "#EF4444", sortOrder = 6),
            Category(id = "cat_notes", name = "Secure Notes", iconName = "EditNote", colorHex = "#14B8A6", sortOrder = 7)
        )
    }
}
