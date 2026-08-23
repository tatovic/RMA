package rs.homeinventory.app.util

fun interface ErrorMessageProvider {
    fun message(code: ErrorCode): String
}
