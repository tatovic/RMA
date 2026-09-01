package rs.homeinventory.app.presentation.itemdetails

import rs.homeinventory.app.domain.model.WarrantyStatus

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
    val imagePath: String?,
    // US-15/BR-013 — vidljivo samo kad se valuta predmeta razlikuje od valute prikaza korisnika.
    val convertedValueFormatted: String?,
    val convertedValueUnavailable: Boolean,
    // BR-010 — status garancije, uvek izveden (tiket 22). Preostali dani su null za ISTEKLA/NEPOZNATO,
    // gde "preostalo dana" nema smisla.
    val warrantyStatus: WarrantyStatus,
    val warrantyDaysRemaining: Int?
)
