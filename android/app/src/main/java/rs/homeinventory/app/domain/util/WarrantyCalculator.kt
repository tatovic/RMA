package rs.homeinventory.app.domain.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import rs.homeinventory.app.domain.model.WarrantyStatus

// tech.md sekcija 8.1/BR-010 — jedino mesto koje racuna status garancije; nikada se ne cuva u bazi
// (db.md sekcija 2.4). Poredjenje je iskljucivo po kalendarskom datumu, bez vremena i vremenske zone.
object WarrantyCalculator {

    fun status(expiration: LocalDate?, thresholdDays: Int, today: LocalDate = LocalDate.now()): WarrantyStatus {
        expiration ?: return WarrantyStatus.NEPOZNATO
        return when {
            expiration.isBefore(today) -> WarrantyStatus.ISTEKLA
            // Datum isteka na danasnji dan (D == T) upada ovde, ne u granu iznad — "uskoro istice", ne "istekla".
            !expiration.isAfter(today.plusDays(thresholdDays.toLong())) -> WarrantyStatus.USKORO_ISTICE
            else -> WarrantyStatus.AKTIVNA
        }
    }

    fun daysRemaining(expiration: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, expiration)
}
