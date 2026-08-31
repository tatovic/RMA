package rs.homeinventory.app.presentation.itemdetails

data class ItemDetailsUi(
    val name: String,
    val categoryName: String,
    val locationName: String,
    val description: String?,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val quantity: Int,
    val purchasePriceFormatted: String?,
    val estimatedValueFormatted: String?,
    val purchaseDateFormatted: String?,
    val warrantyExpirationDateFormatted: String?,
    val seller: String?,
    val notes: String?,
    val imagePath: String?
)
