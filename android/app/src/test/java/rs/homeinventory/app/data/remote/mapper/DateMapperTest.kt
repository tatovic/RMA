package rs.homeinventory.app.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DateMapperTest {

    @Test
    fun `isoToEpochMillis parsira ISO-8601 sa milisekundama`() {
        val iso = "2026-08-22T14:07:31.123Z"
        val expected = Instant.parse(iso).toEpochMilli()

        assertEquals(expected, DateMapper.isoToEpochMillis(iso))
    }

    @Test
    fun `epochMillisToIso formatira uvek sa tri decimale milisekundi`() {
        val millis = Instant.parse("2026-08-22T00:02:31.000Z").toEpochMilli()

        assertEquals("2026-08-22T00:02:31.000Z", DateMapper.epochMillisToIso(millis))
    }

    @Test
    fun `epochMillisToIso i isoToEpochMillis su inverzne operacije`() {
        val original = "2026-01-05T09:15:00.500Z"

        val roundTripped = DateMapper.epochMillisToIso(DateMapper.isoToEpochMillis(original))

        assertEquals(original, roundTripped)
    }

    @Test
    fun `isoToEpochMillisOrNull vraca null za null ulaz`() {
        assertNull(DateMapper.isoToEpochMillisOrNull(null))
    }

    @Test
    fun `epochMillisToIsoOrNull vraca null za null ulaz`() {
        assertNull(DateMapper.epochMillisToIsoOrNull(null))
    }

    @Test
    fun `parseLocalDate parsira YYYY-MM-DD`() {
        assertEquals(LocalDate.of(2024, 3, 15), DateMapper.parseLocalDate("2024-03-15"))
    }

    @Test
    fun `parseLocalDate vraca null za null i neispravan format`() {
        assertNull(DateMapper.parseLocalDate(null))
        assertNull(DateMapper.parseLocalDate("nije-datum"))
    }

    @Test
    fun `formatLocalDate vraca YYYY-MM-DD i null za null ulaz`() {
        assertEquals("2024-03-15", DateMapper.formatLocalDate(LocalDate.of(2024, 3, 15)))
        assertNull(DateMapper.formatLocalDate(null))
    }
}
