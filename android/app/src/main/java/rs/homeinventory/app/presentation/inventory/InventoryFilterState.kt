package rs.homeinventory.app.presentation.inventory

// FR-033 do FR-037 — stanje filtera liste inventara (tiket 20). Raspon cene racuna se kao JEDAN
// aktivan filter cak i kad su oba kraja postavljena (min i max).
data class InventoryFilterState(
    val categoryIds: Set<String> = emptySet(),
    val locationIds: Set<String> = emptySet(),
    val minPriceMinor: Long? = null,
    val maxPriceMinor: Long? = null,
    val purchaseYear: Int? = null,
    val underWarrantyOnly: Boolean = false,
    val warrantyExpiringSoonOnly: Boolean = false
) {
    // Broj aktivnih filtera prikazan na dugmetu za otvaranje panela.
    val activeCount: Int
        get() = listOf(
            categoryIds.isNotEmpty(),
            locationIds.isNotEmpty(),
            minPriceMinor != null || maxPriceMinor != null,
            purchaseYear != null,
            underWarrantyOnly,
            warrantyExpiringSoonOnly
        ).count { it }

    val isEmpty: Boolean get() = activeCount == 0
}
