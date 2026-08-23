package rs.homeinventory.app.data.remote.interceptor

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import rs.homeinventory.app.data.local.prefs.UserPreferences

// Dodaje token iskljucivo zahtevima ka nasem backendu — nikada CurrencyApi klijentu (tech.md sekcija 9).
class AuthInterceptor(private val prefs: UserPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // runBlocking je ovde prihvatljiv jer se interceptor izvrsava na OkHttp niti, nikada na glavnoj.
        val token = runBlocking { prefs.token.first() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}
