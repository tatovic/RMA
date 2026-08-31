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
}
