package rs.homeinventory.app.presentation.inventory

// FR-038 — svih sest nacina sortiranja liste inventara (tiket 20). name se koristi kao kljuc za
// cuvanje izbora u DataStore (InventoryPreferences), pa se ne sme menjati bez migracije.
enum class InventorySortMode {
    NAME_ASC,
    NAME_DESC,
    PRICE_ASC,
    PRICE_DESC,
    NEWEST,
    OLDEST;

    companion object {
        val DEFAULT = NEWEST

        fun fromStorageKey(key: String?): InventorySortMode = entries.find { it.name == key } ?: DEFAULT
    }
}
