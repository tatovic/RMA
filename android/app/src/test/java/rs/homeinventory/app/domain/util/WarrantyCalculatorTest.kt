package rs.homeinventory.app.domain.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import rs.homeinventory.app.domain.model.WarrantyStatus

// BR-010/tech.md sekcija 8.1 — sve cetiri grane i granicni datumi. Poredjenje je iskljucivo po
// kalendarskom datumu, bez vremena i vremenske zone (LocalDate, ne Instant).
class WarrantyCalculatorTest {

    private val today = LocalDate.of(2026, 9, 1)
    private val threshold = 30

    @Test
    fun `nedostajuci datum isteka vraca NEPOZNATO`() {
        assertEquals(WarrantyStatus.NEPOZNATO, WarrantyCalculator.status(null, threshold, today))
    }

    @Test
    fun `datum u proslosti vraca ISTEKLA`() {
        val yesterday = today.minusDays(1)
        assertEquals(WarrantyStatus.ISTEKLA, WarrantyCalculator.status(yesterday, threshold, today))
    }

    @Test
    fun `datum isteka na danasnji dan vraca USKORO_ISTICE, ne ISTEKLA`() {
        assertEquals(WarrantyStatus.USKORO_ISTICE, WarrantyCalculator.status(today, threshold, today))
    }

    @Test
    fun `datum unutar praga vraca USKORO_ISTICE`() {
        val withinThreshold = today.plusDays(15)
        assertEquals(WarrantyStatus.USKORO_ISTICE, WarrantyCalculator.status(withinThreshold, threshold, today))
    }

    @Test
    fun `datum tacno na granici praga vraca USKORO_ISTICE`() {
        val onBoundary = today.plusDays(threshold.toLong())
        assertEquals(WarrantyStatus.USKORO_ISTICE, WarrantyCalculator.status(onBoundary, threshold, today))
    }

    @Test
    fun `datum jedan dan posle granice praga vraca AKTIVNA`() {
        val justPastBoundary = today.plusDays(threshold.toLong() + 1)
        assertEquals(WarrantyStatus.AKTIVNA, WarrantyCalculator.status(justPastBoundary, threshold, today))
    }

    @Test
    fun `datum daleko u buducnosti vraca AKTIVNA`() {
        val farFuture = today.plusYears(2)
        assertEquals(WarrantyStatus.AKTIVNA, WarrantyCalculator.status(farFuture, threshold, today))
    }

    @Test
    fun `granica praga se pomera sa razlicitim vrednostima praga (FR-051)`() {
        val in45Days = today.plusDays(45)
        assertEquals(WarrantyStatus.AKTIVNA, WarrantyCalculator.status(in45Days, 30, today))
        assertEquals(WarrantyStatus.USKORO_ISTICE, WarrantyCalculator.status(in45Days, 60, today))
    }

    @Test
    fun `daysRemaining racuna razliku u kalendarskim danima`() {
        assertEquals(15L, WarrantyCalculator.daysRemaining(today.plusDays(15), today))
        assertEquals(0L, WarrantyCalculator.daysRemaining(today, today))
        assertEquals(-1L, WarrantyCalculator.daysRemaining(today.minusDays(1), today))
    }
}
