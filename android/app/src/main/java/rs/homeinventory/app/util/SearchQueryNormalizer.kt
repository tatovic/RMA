package rs.homeinventory.app.util

import java.util.Locale

// FR-031/FR-032, tech.md 8.5 — upit se normalizuje u Kotlinu (trim + lowercase sa srpskim Locale)
// pre poredjenja, jer SQLite LOWER()/COLLATE NOCASE ne pokrivaju srpske dijakritike. Dodatno se
// dijakritici svode na latinicna slova bez kvacica, da upit "sporet" nadje "Šporet" (tiket 19).
object SearchQueryNormalizer {

    private val SERBIAN_LOCALE = Locale("sr", "RS")

    private val DIACRITICS = mapOf(
        'š' to 's', 'č' to 'c', 'ć' to 'c', 'ž' to 'z', 'đ' to 'd'
    )

    fun normalize(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text.trim().lowercase(SERBIAN_LOCALE)) {
            builder.append(DIACRITICS[char] ?: char)
        }
        return builder.toString()
    }
}
