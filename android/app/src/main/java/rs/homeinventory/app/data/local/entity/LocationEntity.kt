package rs.homeinventory.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import rs.homeinventory.app.data.local.SyncStatus

@Entity(
    tableName = "locations",
    indices = [Index(value = ["userId"]), Index(value = ["userId", "name"], unique = true)]
)
data class LocationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
