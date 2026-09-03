package rs.homeinventory.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// tech.md sekcija 8.6 — provera mreze pre pokusaja sinhronizacije. Pseudokod je ovo trazio od
// pocetka (`if (!networkMonitor.isOnline())`), ali klasa nikad nije napisana, pa je sync offline
// isao u pun ciklus konekcijskih timeout-a: 20 predmeta na cekanju je znacilo 20 x 15s (tiket 28,
// nalaz 05).
//
// Ovo je jeftina provera stanja, ne garancija: mreza moze da otkaze i izmedju provere i zahteva.
// Zato ostaje i puna obrada gresaka u safeApiCall — ovde se samo izbegava ocigledno uzaludan posao.
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
