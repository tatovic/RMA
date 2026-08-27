package rs.homeinventory.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

// tech.md sekcija 8.2 i 13 — RSD i EUR format, jedina funkcija koja sme formatirati novac.
class MoneyFormatterTest {

    @Test
    fun `formatira RSD sa tackom kao separatorom hiljada i zarezom za decimale`() {
        assertEquals("120.000,00 RSD", MoneyFormatter.format(12000000L, "RSD"))
    }

    @Test
    fun `formatira EUR po istom pravilu`() {
        assertEquals("1.022,77 EUR", MoneyFormatter.format(102277L, "EUR"))
    }

    @Test
    fun `nula se formatira sa dve decimale`() {
        assertEquals("0,00 RSD", MoneyFormatter.format(0L, "RSD"))
    }
}
