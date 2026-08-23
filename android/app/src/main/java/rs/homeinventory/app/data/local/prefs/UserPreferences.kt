package rs.homeinventory.app.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rs.homeinventory.app.util.JwtUtils
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

// Token, userId i rola — jedini podaci o sesiji koji se cuvaju lokalno. Lozinka se ne cuva nigde (SEC-10).
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val roleKey = stringPreferencesKey("user_role")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val userId: Flow<String?> = context.dataStore.data.map { it[userIdKey] }
    val role: Flow<String?> = context.dataStore.data.map { it[roleKey] }

    // Poziva se posle uspesne prijave/registracije (FR-018).
    suspend fun saveSession(token: String, userId: String, role: String) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[userIdKey] = userId
            it[roleKey] = role
        }
    }

    // FR-009/FR-010 — poziva se pri 401 (TOKEN_EXPIRED/TOKEN_INVALID) i pri odjavi.
    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(tokenKey)
            it.remove(userIdKey)
            it.remove(roleKey)
        }
    }

    // FR-011 — sesija je vazeca ako token postoji i njegov JWT "exp" jos nije prosao.
    // Potpis se ne verifikuje lokalno (to je odgovornost servera, SEC-02); ovo je samo izbor pocetnog ekrana.
    suspend fun hasValidSession(): Boolean {
        val current = token.first() ?: return false
        val expiration = JwtUtils.expirationMillis(current) ?: return false
        return expiration > System.currentTimeMillis()
    }
}
