package rs.homeinventory.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import rs.homeinventory.app.data.local.SyncStatus

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["userId", "deletedAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["locationId"]),
        Index(value = ["userId", "warrantyExpirationDate"]),
        Index(value = ["syncStatus"]),
        // NFR-01 (tiket 27) — svaki upit za listu inventara filtrira po userId i sortira po createdAt
        // (ItemDao); ovaj indeks pokriva bas tu kombinaciju umesto pune tabelarne pretrage na 500+ redova.
        Index(value = ["userId", "createdAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"], childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"], childColumns = ["locationId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,              // UUID v4, generise klijent
    val userId: String,
    val name: String,
    val description: String?,
    val categoryId: String,
    val locationId: String,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val quantity: Int,
    val purchasePrice: Long?,                // minor jedinice
    val estimatedValue: Long?,               // minor jedinice
    val currency: String,
    val purchaseDate: String?,               // "YYYY-MM-DD"
    val warrantyExpirationDate: String?,     // "YYYY-MM-DD"
    val seller: String?,
    val notes: String?,
    val createdAt: Long,                     // epoch millis
    val updatedAt: Long,                     // epoch millis
    val deletedAt: Long?,                    // soft delete

    // --- SAMO LOKALNO, ne postoji na serveru ---
    val imagePath: String?,                  // naziv fajla u internal storage
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
