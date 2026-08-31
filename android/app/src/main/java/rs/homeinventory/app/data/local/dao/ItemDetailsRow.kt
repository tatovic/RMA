package rs.homeinventory.app.data.local.dao

data class ItemDetailsRow(
    val id: String,
    val name: String,
    val description: String?,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val quantity: Int,
    val purchasePrice: Long?,
    val estimatedValue: Long?,
    val currency: String,
    val purchaseDate: String?,
    val warrantyExpirationDate: String?,
    val seller: String?,
    val notes: String?,
    val imagePath: String?,
    val categoryName: String,
    val locationName: String
)
