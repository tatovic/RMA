package rs.homeinventory.app.presentation.locations

// SCR-10 — model za prikaz jedne lokacije, sa brojem predmeta koji je koriste (FR-049).
data class LocationUi(
    val id: String,
    val name: String,
    val description: String?,
    val itemCount: Int
)
