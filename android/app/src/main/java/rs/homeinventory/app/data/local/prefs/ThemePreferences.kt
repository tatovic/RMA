package rs.homeinventory.app.data.local.prefs

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themePrefsDataStore by preferencesDataStore(name = "theme_prefs")

// Izbor teme u Profilu (svetla/tamna/sistemska), cuva se kao AppCompatDelegate.MODE_NIGHT_*
// konstanta. HomeInventoryApp cita ovu vrednost pri startu aplikacije da bi je primenio pre
// nego sto se prva Activity nacrta.
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nightModeKey = intPreferencesKey("night_mode")

    val nightMode: Flow<Int> = context.themePrefsDataStore.data
        .map { it[nightModeKey] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }

    suspend fun saveNightMode(mode: Int) {
        context.themePrefsDataStore.edit { it[nightModeKey] = mode }
    }
}
