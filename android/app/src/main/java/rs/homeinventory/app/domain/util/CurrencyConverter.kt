package rs.homeinventory.app.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

// tech.md sekcija 7.3 — kursevi su EUR-bazirani (rates[X] = koliko X vredi 1 EUR).
// Konverzija je egzaktna (NFR-12): iskljucivo BigDecimal, nikada Float/Double nad novcem.
object CurrencyConverter {

    fun convert(amountMinor: Long, from: String, to: String, rates: Map<String, Double>): Long? {
        if (from == to) return amountMinor
        val rFrom = rates[from] ?: return null   // BR-013: null, nikada 1.0
        val rTo = rates[to] ?: return null
        if (rFrom <= 0.0 || rTo <= 0.0) return null
        return BigDecimal.valueOf(amountMinor)
            .multiply(BigDecimal.valueOf(rTo))
            .divide(BigDecimal.valueOf(rFrom), 0, RoundingMode.HALF_UP)
            .toLong()
    }
}
