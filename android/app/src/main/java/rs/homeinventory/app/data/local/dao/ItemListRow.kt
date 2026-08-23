package rs.homeinventory.app.data.local.dao

import rs.homeinventory.app.data.local.SyncStatus

data class ItemListRow(
    val id: String,
    val name: String,
    val manufacturer: String?,
    val model: String?,
    val quantity: Int,
    val purchasePrice: Long?,
    val estimatedValue: Long?,
    val currency: String,
    val purchaseDate: String?,
    val warrantyExpirationDate: String?,
    val imagePath: String?,
    val createdAt: Long,
    val syncStatus: SyncStatus,
    val categoryId: String,
    val categoryName: String,
    val categoryIconKey: String?,
    val locationId: String,
    val locationName: String
)
