package rs.homeinventory.app.util

// Stanje celog ekrana — cetiri stanja po BR-017, tech.md sekcija 5.4.
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
