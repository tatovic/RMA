package rs.homeinventory.app.data.remote.mapper

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Konverzija trenutka u vremenu (ISO-8601 <-> epoch millis) postoji na tacno dva mesta —
// ovde i backend/src/utils/serializer.js (db.md sekcija 7). Nigde drugde.
object DateMapper {

    private val instantFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    fun isoToEpochMillis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    fun isoToEpochMillisOrNull(iso: String?): Long? = iso?.let { isoToEpochMillis(it) }

    fun epochMillisToIso(millis: Long): String = instantFormatter.format(Instant.ofEpochMilli(millis))

    fun epochMillisToIsoOrNull(millis: Long?): String? = millis?.let { epochMillisToIso(it) }

    // Kalendarski datum (bez vremena) — db.md sekcija 2.3A, uvek "YYYY-MM-DD", vec leksikografski oblik na Room sloju.
    fun parseLocalDate(value: String?): LocalDate? = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun formatLocalDate(date: LocalDate?): String? = date?.toString()
}
