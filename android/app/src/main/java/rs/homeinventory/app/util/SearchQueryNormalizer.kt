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

    // LIKE dzokeri koje korisnik kuca kao obican tekst (tiket 28, nalaz C7). Bez escapovanja je upit
    // "50%" vracao ceo inventar (`%` znaci "bilo sta"), a "a_b" je nalazio i "axb" (`_` znaci "bilo
    // koji jedan znak") — pretraga je tiho lagala umesto da nadje tacno ono sto je uneto.
    //
    // Escape znak je obrnuta kosa crta i mora da se poklapa sa `ESCAPE '\'` uz svaki LIKE u
    // ItemDao.search. Sama crta se escapuje PRVA, inace bi drugi prolaz udvostrucio vec dodate crte.
    private const val LIKE_ESCAPE_CHAR = '\\'
    private val LIKE_WILDCARDS = charArrayOf('\\', '%', '_')

    fun normalize(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text.trim().lowercase(SERBIAN_LOCALE)) {
            val folded = DIACRITICS[char] ?: char
            if (folded in LIKE_WILDCARDS) builder.append(LIKE_ESCAPE_CHAR)
            builder.append(folded)
        }
        return builder.toString()
    }
}
