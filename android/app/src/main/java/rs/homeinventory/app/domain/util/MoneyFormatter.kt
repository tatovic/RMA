package rs.homeinventory.app.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

// tech.md sekcija 8.2 — jedino mesto koje formatira/parsira novac; String.format nad novcanim vrednostima je zabranjeno.
object MoneyFormatter {
    private val locale = Locale("sr", "RS")
    private const val EXPONENT = 2
    private val PLAIN_DECIMAL = Regex("^\\d+([.,]\\d{1,2})?$")

    fun format(amountMinor: Long, currency: String): String {
        val major = BigDecimal.valueOf(amountMinor).movePointLeft(EXPONENT)
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "${numberFormat.format(major)} $currency"
    }

    // VR-10/VR-11 — cena se unosi u obicnom obliku (bez hiljadarki), zarez ili tacka kao decimalni separator.
    // Vraca null za negativne i neispravne vrednosti — poziv treba da to tretira kao gresku validacije.
    fun parseToMinor(input: String): Long? {
        val trimmed = input.trim()
        if (!PLAIN_DECIMAL.matches(trimmed)) return null
        return runCatching {
            BigDecimal(trimmed.replace(',', '.')).movePointRight(EXPONENT).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()
    }
}
