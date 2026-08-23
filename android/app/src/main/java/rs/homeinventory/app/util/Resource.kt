package rs.homeinventory.app.util

// Rezultat jedne mrezne operacije — tech.md sekcija 5.4.
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val code: ErrorCode, val message: String) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
