package rs.homeinventory.app.data.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rs.homeinventory.app.data.local.HomeInventoryDatabase
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

// FR-009 — odgovor 401 briše sesiju i vraća korisnika na prijavu (tiket 26). Namerno odvojeno od
// AuthRepository: AuthInterceptor mora moci da ga koristi, a AuthRepository zavisi od BackendApi koji
// zavisi od OkHttpClient-a koji zavisi od AuthInterceptor-a (kruzna zavisnost).
@Singleton
class SessionManager @Inject constructor(
    private val prefs: UserPreferences,
    private val database: HomeInventoryDatabase,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    // Vise paralelnih zahteva ume da dobije 401 istovremeno; posao se radi tacno jednom.
    private val mutex = Mutex()

    // Namerno NIJE suspend i namerno ne blokira pozivaoca: jedini pozivalac je AuthInterceptor, koji
    // se izvrsava na OkHttp niti sa jos otvorenim odgovorom u ruci. Ranije je tu, pod runBlocking-om,
    // stajalo brisanje cele Room baze — mrezna nit je cekala na disk I/O dok je telo odgovora visilo
    // (tiket 28, nalaz 10). Sada se posao samo zakazuje na aplikacionom opsegu.
    fun onUnauthorized() {
        applicationScope.launch {
            mutex.withLock {
                // Token vec null znaci da je jedan konkurentan 401 vec obradio isteklu sesiju — ne
                // treba ponovo dirati bazu ni ponovo signalizirati UI.
                if (prefs.token.first() == null) return@withLock

                val userId = prefs.userId.first()
                prefs.clearSession()

                // BR-005 — odjava brise sve, istekla sesija cuva neposlat rad ovog korisnika.
                // Korisnik nije trazio da ode; njegovi PENDING predmeti su jedini primerak tog unosa
                // i posle ponovne prijave se salju serveru.
                database.clearPreservingUnsyncedWork(userId)

                _sessionExpired.emit(Unit)
            }
        }
    }
}
