package rs.homeinventory.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import rs.homeinventory.app.data.local.dao.CategoryAggregate
import rs.homeinventory.app.data.local.dao.CategoryDao
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.LocationDao
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.mapper.toEntity
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

// Jedina tacka pristupa predmetima/kategorijama/lokacijama — DEP-03. Puni Room sa servera;
// UI cita iskljucivo iz Room-a (tech.md sekcija 5.3). Puna povlaka bez delta parametra —
// dvosmerna sinhronizacija dolazi u tiketu 26.
@Singleton
class ItemRepository @Inject constructor(
    private val api: BackendApi,
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val locationDao: LocationDao,
    private val errorMessageProvider: ErrorMessageProvider
) {
    fun observeCategoryAggregates(userId: String): Flow<List<CategoryAggregate>> =
        itemDao.observeCategoryAggregates(userId)

    fun observeRecentItems(userId: String, limit: Int): Flow<List<ItemListRow>> =
        itemDao.observeRecent(userId, limit)

    suspend fun refresh(): Resource<Unit> = withContext(Dispatchers.IO) {
        val categoriesResult = pullAndStore({ api.getCategories() }) { response ->
            categoryDao.upsertAll(response.categories.map { it.toEntity() })
        }
        if (categoriesResult !is Resource.Success) return@withContext categoriesResult

        val locationsResult = pullAndStore({ api.getLocations() }) { response ->
            locationDao.upsertAll(response.locations.map { it.toEntity() })
        }
        if (locationsResult !is Resource.Success) return@withContext locationsResult

        pullAndStore({ api.getItems() }) { response ->
            response.items.forEach { dto ->
                // DB-RULE-02 (FR-085) — imagePath postoji samo lokalno i mora se sacuvati pri pull-u.
                val existingImagePath = itemDao.getById(dto.id)?.imagePath
                itemDao.upsert(dto.toEntity(keepImagePath = existingImagePath))
            }
        }
    }

    private suspend fun <T> pullAndStore(
        call: suspend () -> Response<T>,
        store: suspend (T) -> Unit
    ): Resource<Unit> = when (val result = safeApiCall(errorMessageProvider, call)) {
        is Resource.Success -> {
            store(result.data)
            Resource.Success(Unit)
        }
        is Resource.Error -> Resource.Error(result.code, result.message)
        Resource.Loading -> Resource.Loading
    }
}
