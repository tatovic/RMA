package rs.homeinventory.app.data.remote.interceptor

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.data.session.SessionManager

// Dodaje token iskljucivo zahtevima ka nasem backendu — nikada CurrencyApi klijentu (tech.md sekcija 9).
class AuthInterceptor(
    private val prefs: UserPreferences,
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // runBlocking je ovde prihvatljiv jer se interceptor izvrsava na OkHttp niti, nikada na glavnoj.
        val token = runBlocking { prefs.token.first() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        }
        val response = chain.proceed(request)

        // FR-009 — 401 na zahtevu koji je nosio token znaci da je sesija istekla/nevazeca
        // (TOKEN_EXPIRED/TOKEN_INVALID). Provera tokena razdvaja ovo od login/register, gde 401 znaci
        // pogresne kredencijale (INVALID_CREDENTIALS), ne isteklu sesiju.
        if (response.code == 401 && !token.isNullOrBlank()) {
            runBlocking { sessionManager.onUnauthorized() }
        }
        return response
    }
}
