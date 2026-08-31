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
import rs.homeinventory.app.data.local.dao.LocationWithCount
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.ItemDto
import rs.homeinventory.app.data.remote.dto.LocationDto
import rs.homeinventory.app.data.remote.dto.LocationRequestDto
import rs.homeinventory.app.data.remote.mapper.toDto
import rs.homeinventory.app.data.remote.mapper.toEntity
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.PhotoStorage
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.safeApiCall
import java.util.UUID
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
    private val errorMessageProvider: ErrorMessageProvider,
    private val photoStorage: PhotoStorage
) {
    fun observeCategoryAggregates(userId: String): Flow<List<CategoryAggregate>> =
        itemDao.observeCategoryAggregates(userId)

    fun observeRecentItems(userId: String, limit: Int): Flow<List<ItemListRow>> =
        itemDao.observeRecent(userId, limit)

    fun observeAllItems(userId: String): Flow<List<ItemListRow>> = itemDao.observeAll(userId)

    // SCR-04 — pretraga po sest polja (FR-031, tiket 19); :query mora stici vec normalizovan
    // (SearchQueryNormalizer), search() u ItemDao dodatno svodi kolone na istu golu latinicu.
    fun searchItems(userId: String, normalizedQuery: String): Flow<List<ItemListRow>> =
        itemDao.search(userId, normalizedQuery)

    // SCR-06 — padajuce liste za kategoriju i lokaciju (tiket 15).
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeLocations(userId: String): Flow<List<LocationEntity>> = locationDao.observeAll(userId)

    // SCR-10 — spisak lokacija sa brojem predmeta uz svaku (tiket 17).
    fun observeLocationsWithCount(userId: String): Flow<List<LocationWithCount>> =
        locationDao.observeAllWithItemCount(userId)

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

    // FR-086 — fotografija se trajno brise tek kad opoziv (FR-027) vise nije moguc, da undo ne bi
    // ostavio predmet bez slike. Poziva se posle isteka snackbar-a za opoziv (tiket 16 UI).
    suspend fun finalizeDeletedItemPhoto(id: String): Unit = withContext(Dispatchers.IO) {
        val item = itemDao.getById(id) ?: return@withContext
        if (item.deletedAt != null) photoStorage.delete(item.imagePath)
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

    // SCR-10 — CRUD lokacija (tiket 17). Za razliku od predmeta ovo je mreza-prvo: naziv mora
    // odmah biti proveren kod servera (VR-19), pa se Room azurira tek posle uspesnog odgovora.
    suspend fun createLocation(name: String, description: String?): Resource<Unit> = withContext(Dispatchers.IO) {
        val dto = LocationRequestDto(id = UUID.randomUUID().toString(), name = name, description = description)
        toUnitResource(safeApiCall(errorMessageProvider) { api.createLocation(dto) })
    }

    suspend fun updateLocation(id: String, name: String, description: String?): Resource<Unit> =
        withContext(Dispatchers.IO) {
            val dto = LocationRequestDto(name = name, description = description)
            toUnitResource(safeApiCall(errorMessageProvider) { api.updateLocation(id, dto) })
        }

    private suspend fun toUnitResource(result: Resource<LocationDto>): Resource<Unit> =
        when (result) {
            is Resource.Success -> {
                locationDao.upsert(result.data.toEntity())
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(result.code, result.message)
            Resource.Loading -> Resource.Loading
        }

    // BR-014 se proverava na ekranu (broj predmeta vec je prikazan uz lokaciju) pre ovog poziva;
    // server je konacna potvrda za slucaj trke izmedju uredjaja.
    suspend fun deleteLocation(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        when (val result = safeApiCall(errorMessageProvider) { api.deleteLocation(id) }) {
            is Resource.Success -> {
                locationDao.hardDelete(id)
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(result.code, result.message)
            Resource.Loading -> Resource.Loading
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
