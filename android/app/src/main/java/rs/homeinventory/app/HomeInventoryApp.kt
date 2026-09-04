package rs.homeinventory.app

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rs.homeinventory.app.data.local.prefs.ThemePreferences
import rs.homeinventory.app.data.session.SessionManager
import rs.homeinventory.app.ui.AuthenticationActivity
import javax.inject.Inject

@HiltAndroidApp
class HomeInventoryApp : Application() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var themePreferences: ThemePreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Mora biti primenjeno pre nego sto se prva Activity nacrta, zato blokira ovde umesto da
        // se cita reaktivno kao ostala podesavanja — bez ovoga bi se app kratko prikazao u pogresnoj
        // temi pa preskocio u sacuvanu, sto se vidi kao "treperenje" pri hladnom pokretanju.
        AppCompatDelegate.setDefaultNightMode(runBlocking { themePreferences.nightMode.first() })

        // FR-009 — moze biti signalizirano sa bilo kog ekrana (svaki poziv ka backendu ide kroz
        // AuthInterceptor), zato se osluskuje centralno umesto po Activity-ju.
        appScope.launch {
            sessionManager.sessionExpired.collect {
                val intent = Intent(this@HomeInventoryApp, AuthenticationActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
        }
    }
}
