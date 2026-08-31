package rs.homeinventory.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.dao.CategoryAggregate
import rs.homeinventory.app.data.local.dao.CategoryDao
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.dao.ItemDetailsRow
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.LocationDao
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.ItemDto
import rs.homeinventory.app.data.remote.mapper.toDto
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

    fun observeAllItems(userId: String): Flow<List<ItemListRow>> = itemDao.observeAll(userId)

    // SCR-06 — padajuce liste za kategoriju i lokaciju (tiket 15).
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeLocations(userId: String): Flow<List<LocationEntity>> = locationDao.observeAll(userId)

    suspend fun getItem(id: String): InventoryItemEntity? = itemDao.getById(id)

    // SCR-07 — detalji predmeta (tiket 16), join sa kategorijom/lokacijom kao kod liste.
    fun observeItemDetails(id: String): Flow<ItemDetailsRow?> = itemDao.observeDetails(id)

    // Brisanje predmeta (FR-025/FR-026) — soft delete lokalno pa odmah pokusaj slanja serveru.
    // Neuspeh slanja ne blokira korisnika; predmet ostaje PENDING_DELETE do pune sinhronizacije (tiket 26).
    suspend fun deleteItem(id: String): Unit = withContext(Dispatchers.IO) {
        itemDao.softDelete(id, System.currentTimeMillis())
        safeApiCall(errorMessageProvider) { api.deleteItem(id) }
    }

    // Opoziv brisanja u roku od pet sekundi (FR-027).
    suspend fun undoDelete(id: String): Unit = withContext(Dispatchers.IO) {
        itemDao.undoDelete(id)
    }

    // Cuvanje predmeta (FR-029, FR-030) — upisuje lokalno pa odmah pokusava jedan poziv ka serveru.
    // Neuspeh slanja ne blokira korisnika, predmet ostaje PENDING_* do pune sinhronizacije (tiket 26).
    suspend fun saveItem(entity: InventoryItemEntity, isCreate: Boolean): Unit = withContext(Dispatchers.IO) {
        itemDao.upsert(entity)

        val call: suspend () -> Response<ItemDto> = if (isCreate) {
            { api.createItem(entity.toDto()) }
        } else {
            { api.updateItem(entity.id, entity.toDto()) }
        }

        val result = safeApiCall(errorMessageProvider, call)
        if (result is Resource.Success) {
            itemDao.upsert(result.data.toEntity(keepImagePath = entity.imagePath, syncStatus = SyncStatus.SYNCED))
        }
    }

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
