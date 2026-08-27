package rs.homeinventory.app.domain.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// tech.md sekcija 8.2 — jedino mesto koje formatira novac; String.format nad novcanim vrednostima je zabranjeno.
object MoneyFormatter {
    private val locale = Locale("sr", "RS")
    private const val EXPONENT = 2

    fun format(amountMinor: Long, currency: String): String {
        val major = BigDecimal.valueOf(amountMinor).movePointLeft(EXPONENT)
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "${numberFormat.format(major)} $currency"
    }
}
