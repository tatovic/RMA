package rs.homeinventory.app.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.dao.SyncMetadataDao
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.SyncMetadataEntity
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.ItemDto
import rs.homeinventory.app.data.remote.mapper.toDto
import rs.homeinventory.app.data.remote.mapper.toEntity
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.PhotoStorage
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SYNC_METADATA_ITEMS_LAST_SYNC_KEY
import rs.homeinventory.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

// tech.md sekcija 8.6 — puna dvosmerna sinhronizacija predmeta (tiket 26). Jedina tacka koja pomera
// predmete izmedju Room-a i servera; ItemRepository vise ne radi ad-hoc pojedinacne pozive.
@Singleton
class SyncManager @Inject constructor(
    private val api: BackendApi,
    private val itemDao: ItemDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val photoStorage: PhotoStorage,
    private val errorMessageProvider: ErrorMessageProvider
) {
    // FR-097 — neuspeh se vraca pozivaocu kao Resource.Error, ali svaka lokalna izmena koja je do tog
    // trenutka uspela ostaje upisana u Room; ekran se nikad ne prazni jer je Room izvor istine.
    suspend fun sync(): Resource<Unit> = withContext(Dispatchers.IO) {
        // Pravilo br. 1 (tech.md 8.6) — push ide UVEK pre pull-a, inace bi serverska verzija pregazila
        // lokalne izmene koje jos nisu poslate.
        val pushError = push()
        when (val pullResult = pull()) {
            is Resource.Error -> pullResult
            else -> pushError ?: Resource.Success(Unit)
        }
    }

    private suspend fun push(): Resource.Error? {
        var firstError: Resource.Error? = null
        itemDao.getPending().forEach { item ->
            val error = when (item.syncStatus) {
                SyncStatus.PENDING_CREATE -> pushCreate(item)
                SyncStatus.PENDING_UPDATE -> pushUpdate(item)
                SyncStatus.PENDING_DELETE -> pushDelete(item)
                SyncStatus.SYNCED -> null
            }
            if (firstError == null) firstError = error
        }
        return firstError
    }

    // DB-RULE-03 — predmet kreiran i obrisan pre nego sto je server za njega saznao se brise lokalno,
    // bez ijednog poziva serveru (ItemDao.softDelete namerno ostavlja ovakav predmet u PENDING_CREATE).
    private suspend fun pushCreate(item: InventoryItemEntity): Resource.Error? {
        if (item.deletedAt != null) {
            photoStorage.delete(item.imagePath)
            itemDao.hardDelete(item.id)
            return null
        }
        return when (val result = safeApiCall(errorMessageProvider) { api.createItem(item.toDto()) }) {
            is Resource.Success -> {
                // DB-RULE-02 (FR-085) — imagePath postoji samo lokalno, cuva se eksplicitno pri svakom upisu sa servera.
                itemDao.upsert(result.data.toEntity(keepImagePath = item.imagePath, syncStatus = SyncStatus.SYNCED))
                null
            }
            is Resource.Error -> result
            Resource.Loading -> null
        }
    }

    private suspend fun pushUpdate(item: InventoryItemEntity): Resource.Error? =
        when (val result = safeApiCall(errorMessageProvider) { api.updateItem(item.id, item.toDto()) }) {
            is Resource.Success -> {
                itemDao.upsert(result.data.toEntity(keepImagePath = item.imagePath, syncStatus = SyncStatus.SYNCED))
                null
            }
            is Resource.Error -> if (result.code == ErrorCode.SYNC_CONFLICT) resolveConflict(item) else result
            Resource.Loading -> null
        }

    // DB-RULE-04 — server je odbio jer poseduje noviju verziju; ta verzija prepisuje lokalnu (uz
    // ocuvanje imagePath, DB-RULE-02). Backend vraca konfliktnu verziju direktno u telu 409 odgovora
    // (items.service.js), ali safeApiCall namerno ne prosledjuje `details` pozivaocu (jedino mesto koje
    // parsira gresku, ERR-01/ERR-03) — zato se ovde svesno radi jedan dodatni GET umesto novog kanala
    // za citanje sirovog tela greske.
    private suspend fun resolveConflict(item: InventoryItemEntity): Resource.Error? =
        when (val result = safeApiCall(errorMessageProvider) { api.getItem(item.id) }) {
            is Resource.Success -> {
                itemDao.upsert(result.data.toEntity(keepImagePath = item.imagePath, syncStatus = SyncStatus.SYNCED))
                null
            }
            is Resource.Error -> result
            Resource.Loading -> null
        }

    private suspend fun pushDelete(item: InventoryItemEntity): Resource.Error? =
        when (val result = safeApiCall(errorMessageProvider) { api.deleteItem(item.id) }) {
            is Resource.Success -> {
                photoStorage.delete(item.imagePath) // FR-086
                itemDao.hardDelete(item.id)
                null
            }
            is Resource.Error -> result
            Resource.Loading -> null
        }

    // Delta preko `since` (FR-098) — vraca i tombstone redove, pa brisanje sa drugog uredjaja stigne i ovde.
    private suspend fun pull(): Resource<Unit> {
        val since = syncMetadataDao.get(SYNC_METADATA_ITEMS_LAST_SYNC_KEY)?.value
        return when (val result = safeApiCall(errorMessageProvider) { api.getItems(since = since) }) {
            is Resource.Success -> {
                result.data.items.forEach { dto -> applyRemoteItem(dto) }
                // Serversko vreme se cuva za sledeci delta pull, nikad System.currentTimeMillis() —
                // sat na telefonu moze biti pogresan.
                syncMetadataDao.upsert(SyncMetadataEntity(SYNC_METADATA_ITEMS_LAST_SYNC_KEY, result.data.serverTime))
                Resource.Success(Unit)
            }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
    }

    // Lokalna izmena koja jos nije poslata ima prednost nad serverskom verzijom dok se ne posalje —
    // pull je preskace umesto da ih prepise.
    private suspend fun applyRemoteItem(dto: ItemDto) {
        val local = itemDao.getById(dto.id)
        if (local != null && local.syncStatus != SyncStatus.SYNCED) return
        if (dto.deletedAt != null) {
            photoStorage.delete(local?.imagePath)
            itemDao.hardDelete(dto.id)
        } else {
            // DB-RULE-02 (FR-085) — imagePath se ne prepisuje sa servera.
            itemDao.upsert(dto.toEntity(keepImagePath = local?.imagePath))
        }
    }
}
