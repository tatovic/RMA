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

private val Context.inventoryPrefsDataStore by preferencesDataStore(name = "inventory_prefs")

// US-12 — izabrano sortiranje liste inventara se pamti izmedju pokretanja aplikacije, za razliku od
// filtera koji zive samo u SavedStateHandle-u (tiket 20).
@Singleton
class InventoryPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sortModeKey = stringPreferencesKey("sort_mode")

    val sortMode: Flow<String?> = context.inventoryPrefsDataStore.data.map { it[sortModeKey] }

    suspend fun saveSortMode(storageKey: String) {
        context.inventoryPrefsDataStore.edit { it[sortModeKey] = storageKey }
    }
}
