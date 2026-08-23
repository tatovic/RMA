package rs.homeinventory.app.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

// Token, ne loguje se i ne cuva nigde drugde (SEC-10).
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    // FR-009 — poziva se pri 401 (TOKEN_EXPIRED/TOKEN_INVALID) i pri odjavi.
    suspend fun clearToken() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
