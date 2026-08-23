package rs.homeinventory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import rs.homeinventory.app.data.local.UserRole

// Kesira SAMO trenutno prijavljenog korisnika. Uvek najvise jedan red.
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole,          // TypeConverter
    val isActive: Boolean,
    val currency: String,        // valuta prikaza
    val createdAt: Long,
    val updatedAt: Long
)
