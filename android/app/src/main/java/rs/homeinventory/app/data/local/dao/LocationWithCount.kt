package rs.homeinventory.app.data.local.dao

// SCR-10 — lokacija sa brojem predmeta koji je koriste (FR-049, tiket 17).
data class LocationWithCount(
    val id: String,
    val name: String,
    val description: String?,
    val itemCount: Int
)
