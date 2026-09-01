package rs.homeinventory.app.data.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import rs.homeinventory.app.data.local.HomeInventoryDatabase
import rs.homeinventory.app.data.local.prefs.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

// FR-009 — odgovor 401 briše sesiju i lokalnu bazu i vraća korisnika na prijavu (tiket 26). Namerno
// odvojeno od AuthRepository: AuthInterceptor mora moci da ga koristi, a AuthRepository zavisi od
// BackendApi koji zavisi od OkHttpClient-a koji zavisi od AuthInterceptor-a (kruzna zavisnost).
@Singleton
class SessionManager @Inject constructor(
    private val prefs: UserPreferences,
    private val database: HomeInventoryDatabase
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    // Token vec null znaci da je jedan konkurentan 401 vec obradio isteklu sesiju — ne treba ponovo
    // brisati bazu ni ponovo signalizirati UI.
    suspend fun onUnauthorized() {
        if (prefs.token.first() == null) return
        prefs.clearSession()
        database.clearAllData()
        _sessionExpired.emit(Unit)
    }
}
