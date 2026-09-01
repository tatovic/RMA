package rs.homeinventory.app.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rs.homeinventory.app.util.WARRANTY_THRESHOLD_DEFAULT_DAYS
import javax.inject.Inject
import javax.inject.Singleton

private val Context.warrantyPrefsDataStore by preferencesDataStore(name = "warranty_prefs")

// FR-051/FR-052 — prag "garancija uskoro istice" biran u Profilu, prezivljava izmedju pokretanja aplikacije.
@Singleton
class WarrantyPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val thresholdDaysKey = intPreferencesKey("warranty_threshold_days")

    val thresholdDays: Flow<Int> = context.warrantyPrefsDataStore.data
        .map { it[thresholdDaysKey] ?: WARRANTY_THRESHOLD_DEFAULT_DAYS }

    suspend fun saveThresholdDays(days: Int) {
        context.warrantyPrefsDataStore.edit { it[thresholdDaysKey] = days }
    }
}
