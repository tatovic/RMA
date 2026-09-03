package rs.homeinventory.app.util

import androidx.annotation.StringRes
import rs.homeinventory.app.R
import rs.homeinventory.app.domain.util.MoneyFormatter
import java.time.LocalDate

// Klijentska validacija forme za dodavanje/izmenu predmeta — prd.md sekcija 9, VR-06 do VR-17, VR-20, BR-016.
object ItemValidator {

    private const val MAX_MINOR = 99_999_999_999L // 999.999.999,99

    data class Input(
        val name: String,
        val categoryId: String?,
        val locationId: String?,
        val description: String,
        val manufacturer: String,
        val model: String,
        val serialNumber: String,
        val quantityRaw: String,
        val purchasePriceRaw: String,
        val estimatedValueRaw: String,
        val currency: String,
        val purchaseDate: LocalDate?,
        val warrantyExpirationDate: LocalDate?,
        val seller: String,
        val notes: String
    )

    data class Errors(
        @StringRes val name: Int? = null,
        @StringRes val category: Int? = null,
        @StringRes val location: Int? = null,
        @StringRes val quantity: Int? = null,
        @StringRes val purchasePrice: Int? = null,
        @StringRes val estimatedValue: Int? = null,
        @StringRes val currency: Int? = null,
        @StringRes val purchaseDate: Int? = null,
        @StringRes val warrantyExpirationDate: Int? = null,
        @StringRes val serialNumber: Int? = null,
        @StringRes val description: Int? = null,
        @StringRes val notes: Int? = null,
        @StringRes val manufacturer: Int? = null,
        @StringRes val model: Int? = null,
        @StringRes val seller: Int? = null
    ) {
        val isValid: Boolean
            get() = listOf(
                name, category, location, quantity, purchasePrice, estimatedValue, currency,
                purchaseDate, warrantyExpirationDate, serialNumber, description, notes,
                manufacturer, model, seller
            ).all { it == null }
    }

    data class Parsed(
        val name: String,
        val categoryId: String,
        val locationId: String,
        val description: String?,
        val manufacturer: String?,
        val model: String?,
        val serialNumber: String?,
        val quantity: Int,
        val purchasePrice: Long?,
        val estimatedValue: Long?,
        val currency: String,
        val purchaseDate: LocalDate?,
        val warrantyExpirationDate: LocalDate?,
        val seller: String?,
        val notes: String?
    )

    // Vraca greske i, ako je unos ispravan, parsirane/normalizovane vrednosti spremne za cuvanje.
    fun validate(input: Input): Pair<Errors, Parsed?> {
        val name = input.name.trim() // VR-20 — trim pre validacije.
        val nameError = if (name.length !in 2..120) R.string.error_vr_item_name else null

        val categoryError = if (input.categoryId.isNullOrBlank()) R.string.error_vr_item_category else null
        val locationError = if (input.locationId.isNullOrBlank()) R.string.error_vr_item_location else null

        val quantity = input.quantityRaw.trim().toIntOrNull()
        val quantityError = if (quantity == null || quantity !in 1..9999) R.string.error_vr_item_quantity else null

        val (purchasePrice, purchasePriceError) = parseMoney(input.purchasePriceRaw, R.string.error_vr_item_purchase_price)
        val (estimatedValue, estimatedValueError) =
            parseMoney(input.estimatedValueRaw, R.string.error_vr_item_estimated_value)

        // VR-12 — jedna lista podrzanih valuta za celu aplikaciju (Constants.kt). Ranije je ovde
        // stajao privatni duplikat iste sestorke, koji bi pri izmeni ostao neazuriran (tiket 28, nalaz C11).
        val currencyError = if (input.currency !in SUPPORTED_CURRENCIES) R.string.error_vr_item_currency else null

        val purchaseDateError =
            if (input.purchaseDate != null && input.purchaseDate.isAfter(LocalDate.now())) {
                R.string.error_vr_item_purchase_date
            } else {
                null
            }

        val warrantyError =
            if (input.warrantyExpirationDate != null && input.purchaseDate != null &&
                input.warrantyExpirationDate.isBefore(input.purchaseDate)
            ) {
                R.string.error_vr_item_warranty_date
            } else {
                null
            }

        val serialNumber = input.serialNumber.trim()
        val serialNumberError = if (serialNumber.length > 100) R.string.error_vr_item_text_100 else null

        val description = input.description.trim()
        val descriptionError = if (description.length > 1000) R.string.error_vr_item_text_1000 else null

        val notes = input.notes.trim()
        val notesError = if (notes.length > 1000) R.string.error_vr_item_text_1000 else null

        val manufacturer = input.manufacturer.trim()
        val manufacturerError = if (manufacturer.length > 100) R.string.error_vr_item_text_100 else null

        val model = input.model.trim()
        val modelError = if (model.length > 100) R.string.error_vr_item_text_100 else null

        val seller = input.seller.trim()
        val sellerError = if (seller.length > 100) R.string.error_vr_item_text_100 else null

        val errors = Errors(
            name = nameError,
            category = categoryError,
            location = locationError,
            quantity = quantityError,
            purchasePrice = purchasePriceError,
            estimatedValue = estimatedValueError,
            currency = currencyError,
            purchaseDate = purchaseDateError,
            warrantyExpirationDate = warrantyError,
            serialNumber = serialNumberError,
            description = descriptionError,
            notes = notesError,
            manufacturer = manufacturerError,
            model = modelError,
            seller = sellerError
        )

        if (!errors.isValid) return errors to null

        // BR-016 — prazno opciono polje se cuva kao odsustvo vrednosti (null), ne kao prazan string.
        val parsed = Parsed(
            name = name,
            categoryId = requireNotNull(input.categoryId),
            locationId = requireNotNull(input.locationId),
            description = description.ifBlank { null },
            manufacturer = manufacturer.ifBlank { null },
            model = model.ifBlank { null },
            serialNumber = serialNumber.ifBlank { null },
            quantity = requireNotNull(quantity),
            purchasePrice = purchasePrice,
            estimatedValue = estimatedValue,
            currency = input.currency,
            purchaseDate = input.purchaseDate,
            warrantyExpirationDate = input.warrantyExpirationDate,
            seller = seller.ifBlank { null },
            notes = notes.ifBlank { null }
        )
        return errors to parsed
    }

    // VR-10/VR-11 — opciono, ne negativno, max 999.999.999,99. Prazno polje je ispravno (nema vrednosti).
    private fun parseMoney(raw: String, @StringRes errorRes: Int): Pair<Long?, Int?> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null to null
        // MoneyFormatter.parseToMinor vec odbija znak minus (regex ne dozvoljava negativan unos), pa
        // je provera `minor < 0` bila mrtva grana — uklonjena u tiketu 28 (nalaz C11).
        val minor = MoneyFormatter.parseToMinor(trimmed)
        return if (minor == null || minor > MAX_MINOR) null to errorRes else minor to null
    }
}
