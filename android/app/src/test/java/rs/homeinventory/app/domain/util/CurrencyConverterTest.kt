package rs.homeinventory.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// tech.md sekcija 7.3/13 — konverzija, nedostajuci kurs i zaokruzivanje.
class CurrencyConverterTest {

    private val rates = mapOf(
        "EUR" to 1.0,
        "RSD" to 117.328652,
        "USD" to 1.16819
    )

    @Test
    fun `ista valuta vraca isti iznos bez racunanja kursa`() {
        assertEquals(12000000L, CurrencyConverter.convert(12000000L, "RSD", "RSD", rates))
    }

    @Test
    fun `konvertuje RSD u EUR po verifikovanom primeru iz tech md`() {
        // 120.000,00 RSD ≈ 1.022,77 EUR (tech.md sekcija 7.3).
        assertEquals(102277L, CurrencyConverter.convert(12000000L, "RSD", "EUR", rates))
    }

    @Test
    fun `konvertuje EUR u USD`() {
        assertEquals(11681900L, CurrencyConverter.convert(10000000L, "EUR", "USD", rates))
    }

    @Test
    fun `nedostajuci kurs izvorisne valute vraca null umesto kursa 1`() {
        assertNull(CurrencyConverter.convert(1000L, "GBP", "EUR", rates))
    }

    @Test
    fun `nedostajuci kurs ciljne valute vraca null umesto kursa 1`() {
        assertNull(CurrencyConverter.convert(1000L, "EUR", "GBP", rates))
    }

    @Test
    fun `prazna mapa kurseva vraca null za bilo koju konverziju osim iste valute`() {
        assertNull(CurrencyConverter.convert(1000L, "RSD", "EUR", emptyMap()))
    }

    @Test
    fun `nulti ili negativan kurs se tretira kao nedostupan`() {
        val brokenRates = mapOf("EUR" to 1.0, "RSD" to 0.0)
        assertNull(CurrencyConverter.convert(1000L, "RSD", "EUR", brokenRates))
    }

    @Test
    fun `zaokruzuje na najbliži ceo broj minor jedinica sa HALF_UP`() {
        // 1 * (1.0 / 3.0) = 0,333... -> zaokruzuje nadole na 0.
        val down = mapOf("A" to 3.0, "B" to 1.0)
        assertEquals(0L, CurrencyConverter.convert(1L, "A", "B", down))

        // 1 * (1.0 / 2.0) = 0,5 -> granicni slucaj, HALF_UP zaokruzuje nagore na 1.
        val boundary = mapOf("A" to 2.0, "B" to 1.0)
        assertEquals(1L, CurrencyConverter.convert(1L, "A", "B", boundary))

        // 5 * (0.5 / 1.0) = 2,5 -> HALF_UP zaokruzuje nagore na 3.
        val half = mapOf("A" to 1.0, "B" to 0.5)
        assertEquals(3L, CurrencyConverter.convert(5L, "A", "B", half))
    }
}
