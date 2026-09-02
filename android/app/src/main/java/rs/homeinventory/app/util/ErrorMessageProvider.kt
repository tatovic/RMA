package rs.homeinventory.app.util

fun interface ErrorMessageProvider {
    // vararg umesto preopterecenja — CATEGORY_IN_USE/LOCATION_IN_USE nose broj predmeta iz
    // details polja servera (prd.md sekcija 10, "{n} predmeta"), ostali kodovi ne prosledjuju nista.
    fun message(code: ErrorCode, vararg formatArgs: Any): String
}
