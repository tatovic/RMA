package rs.homeinventory.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryNormalizerTest {

    @Test
    fun `trim i lowercase se primenjuju`() {
        assertEquals("sto", SearchQueryNormalizer.normalize("  Sto  "))
    }

    @Test
    fun `srpski dijakritici se svode na osnovna slova`() {
        assertEquals("sporet", SearchQueryNormalizer.normalize("Šporet"))
        assertEquals("cokolada", SearchQueryNormalizer.normalize("Čokolada"))
        assertEquals("cup", SearchQueryNormalizer.normalize("Ćup"))
        assertEquals("zar", SearchQueryNormalizer.normalize("Žar"))
        assertEquals("dak", SearchQueryNormalizer.normalize("Đak"))
    }

    // Tiket 28, nalaz C7 — `%` i `_` su LIKE dzokeri; bez escapovanja je "50%" vracao ceo inventar.
    @Test
    fun `LIKE dzokeri iz korisnickog unosa se escapuju`() {
        assertEquals("50\\%", SearchQueryNormalizer.normalize("50%"))
        assertEquals("a\\_b", SearchQueryNormalizer.normalize("a_b"))
    }

    // Sama obrnuta kosa crta je escape znak, pa i ona mora da se udvoji — inace bi upit "c:\\temp"
    // proglasio "t" za escapovan znak i promasio.
    @Test
    fun `obrnuta kosa crta se escapuje sama sobom`() {
        assertEquals("c:\\\\temp", SearchQueryNormalizer.normalize("C:\\temp"))
    }

    @Test
    fun `obican unos ostaje netaknut`() {
        assertEquals("bosch masina", SearchQueryNormalizer.normalize("Bosch Mašina"))
    }
}
