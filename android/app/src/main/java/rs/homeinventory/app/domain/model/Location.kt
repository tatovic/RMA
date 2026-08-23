package rs.homeinventory.app.domain.model

data class Location(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?
)
