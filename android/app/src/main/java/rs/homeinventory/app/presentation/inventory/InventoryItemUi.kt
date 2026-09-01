package rs.homeinventory.app.presentation.inventory

import rs.homeinventory.app.domain.model.WarrantyStatus

// SCR-04 — model reda liste; formatiranje ide u InventoryViewModel iz ItemListRow (BR-011).
data class InventoryItemUi(
    val id: String,
    val name: String,
    val categoryName: String,
    val locationName: String,
    val priceFormatted: String,
    val imagePath: String?,
    val categoryIconKey: String?,
    // FR-055/BR-010 — status garancije, uvek izveden, nikad sacuvan (tiket 22).
    val warrantyStatus: WarrantyStatus
)
