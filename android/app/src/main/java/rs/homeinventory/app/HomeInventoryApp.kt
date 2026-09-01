package rs.homeinventory.app

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.session.SessionManager
import rs.homeinventory.app.ui.AuthenticationActivity
import javax.inject.Inject

@HiltAndroidApp
class HomeInventoryApp : Application() {

    @Inject lateinit var sessionManager: SessionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
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
