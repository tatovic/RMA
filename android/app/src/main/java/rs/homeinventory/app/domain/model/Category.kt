package rs.homeinventory.app.domain.model

data class Category(
    val id: String,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val sortOrder: Int
)
