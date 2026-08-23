package rs.homeinventory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Kes globalnih kategorija sa servera. Klijent ih nikada ne menja.
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val sortOrder: Int,
    val updatedAt: Long
)
