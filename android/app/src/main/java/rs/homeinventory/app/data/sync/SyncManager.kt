package rs.homeinventory.app.data.sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.dao.CategoryDao
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.dao.LocationDao
import rs.homeinventory.app.data.local.dao.SyncMetadataDao
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.SyncMetadataEntity
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.ItemDto
import rs.homeinventory.app.data.remote.mapper.DateMapper
import rs.homeinventory.app.data.remote.mapper.toDto
import rs.homeinventory.app.data.remote.mapper.toEntity
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.NetworkMonitor
import rs.homeinventory.app.util.PhotoStorage
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SYNC_METADATA_ITEMS_LAST_SYNC_KEY
import rs.homeinventory.app.util.SYNC_MAX_PULL_PAGES
import rs.homeinventory.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncManager"

// tech.md sekcija 8.6 — puna dvosmerna sinhronizacija predmeta (tiket 26). Jedina tacka koja pomera
// predmete izmedju Room-a i servera; ItemRepository vise ne radi ad-hoc pojedinacne pozive.
@Singleton
class SyncManager @Inject constructor(
    private val api: BackendApi,
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val locationDao: LocationDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val prefs: UserPreferences,
    private val photoStorage: PhotoStorage,
    private val networkMonitor: NetworkMonitor,
    private val errorMessageProvider: ErrorMessageProvider
) {
    // Preklapanje sinhronizacija nije teorijsko: i Dashboard i Inventar zovu refresh() u init{}, a
    // svako cuvanje predmeta pokrece jos jednu. Dva paralelna prolaza dele isti `since` watermark i
    // isti skup PENDING redova, pa bi jedan slao ono sto je drugi vec poslao (tiket 28, nalaz 08).
    private val syncMutex = Mutex()

    // FR-097 — neuspeh se vraca pozivaocu kao Resource.Error, ali svaka lokalna izmena koja je do tog
    // trenutka uspela ostaje upisana u Room; ekran se nikad ne prazni jer je Room izvor istine.
    suspend fun sync(): Resource<Unit> = withContext(Dispatchers.IO) {
        // tech.md 8.6 — offline se prekida odmah, a ne kroz N konekcijskih timeout-a (tiket 28, nalaz 05).
        if (!networkMonitor.isOnline()) {
            return@withContext Resource.Error(ErrorCode.NO_NETWORK, errorMessageProvider.message(ErrorCode.NO_NETWORK))
        }

        syncMutex.withLock {
            // Redosled kategorije -> lokacije -> predmeti zivi OVDE, a ne u ItemRepository.refresh(),
            // jer saveItem()/deleteItem() zovu sync() direktno i taj put je ranije preskakao punjenje
            // kategorija/lokacija. Predmet sa servera koji pokazuje na lokaciju koju Room jos ne zna
            // obara strani kljuc (tiket 28, blokirajuci nalaz 04).
            val referenceError = pullReferenceData()
            if (referenceError != null) return@withLock referenceError

            // Pravilo br. 1 (tech.md 8.6) — push ide UVEK pre pull-a, inace bi serverska verzija pregazila
            // lokalne izmene koje jos nisu poslate.
            val pushError = push()
            when (val pullResult = pull()) {
                is Resource.Error -> pullResult
                else -> pushError ?: Resource.Success(Unit)
            }
        }
    }

    // Kategorije i lokacije su jednosmerne (server je njihov jedini izvor istine, OWN-05) — samo se
    // pune u Room, nikad ne idu nazad.
    private suspend fun pullReferenceData(): Resource.Error? {
        pullAndStore({ api.getCategories() }) { response ->
            categoryDao.upsertAll(response.categories.map { it.toEntity() })
        }?.let { return it }

        return pullAndStore({ api.getLocations() }) { response ->
            locationDao.upsertAll(response.locations.map { it.toEntity() })
        }
    }

    private suspend fun <T> pullAndStore(
        call: suspend () -> Response<T>,
        store: suspend (T) -> Unit
    ): Resource.Error? = when (val result = safeApiCall(errorMessageProvider, call)) {
        is Resource.Success -> {
            store(result.data)
            null
        }
        is Resource.Error -> result
        Resource.Loading -> null
    }

    private suspend fun push(): Resource.Error? {
        // Bez prijavljenog korisnika nema cijim tokenom da se salje — pull ispod ce svakako pasti na 401.
        val userId = prefs.userId.first() ?: return null

        var firstError: Resource.Error? = null
        for (item in itemDao.getPending(userId)) {
            val error = when (item.syncStatus) {
                SyncStatus.PENDING_CREATE -> pushCreate(item)
                SyncStatus.PENDING_UPDATE -> pushUpdate(item)
                SyncStatus.PENDING_DELETE -> pushDelete(item)
                SyncStatus.SYNCED -> null
            }
            if (error != null) {
                if (firstError == null) firstError = error
                if (stopsPushLoop(error.code)) {
                    Log.w(TAG, "Push prekinut na predmetu ${item.id}: ${error.code}")
                    break
                }
            }
        }
        return firstError
    }

    // Dve grupe gresaka posle kojih nema smisla gadjati server jos jednom u istom prolazu:
    //  - transportne (NO_NETWORK/TIMEOUT/SERVER_UNAVAILABLE) ne govore NISTA o preostalim predmetima,
    //    a svaki sledeci pokusaj kosta jos jedan pun timeout — 20 predmeta offline je znacilo 20 x 15s
    //    cekanja (tiket 28, nalaz 05);
    //  - trajne 4xx (VALIDATION_ERROR/FORBIDDEN) server nece prihvatiti ni za sat vremena. Red ostaje
    //    PENDING i ceka korisnika; petlja staje da ne bi svaki sync ponavljao isti neuspeh (nalaz 06).
    //
    // Posledica koju treba znati: takav "otrovan" red je prvi u getPending() i pri sledecem sync-u
    // ponovo zaustavlja petlju, pa predmeti iza njega cekaju dok ga korisnik ne ispravi. Puno resenje
    // je polje sa greskom po redu i oznaka "trazi paznju" u listi — svesno van opsega tiketa 28.
    private fun stopsPushLoop(code: ErrorCode): Boolean = when (code) {
        ErrorCode.NO_NETWORK,
        ErrorCode.TIMEOUT,
        ErrorCode.SERVER_UNAVAILABLE,
        ErrorCode.VALIDATION_ERROR,
        ErrorCode.FORBIDDEN -> true
        else -> false
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
            is Resource.Error -> when (result.code) {
                ErrorCode.SYNC_CONFLICT -> resolveConflict(item)
                // Server tvrdi da red ne postoji — nema sta da se azurira i nema se cemu vratiti.
                // Klijent i server se zapravo slazu oko ishoda, pa se red uklanja lokalno umesto da
                // zauvek ostane PENDING_UPDATE koji pada na svakom sync-u (tiket 28, nalaz 06).
                ErrorCode.NOT_FOUND -> discardLocally(item)
                else -> result
            }
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
            is Resource.Error -> if (result.code == ErrorCode.NOT_FOUND) discardLocally(item) else result
            Resource.Loading -> null
        }

    private suspend fun pushDelete(item: InventoryItemEntity): Resource.Error? =
        when (val result = safeApiCall(errorMessageProvider) { api.deleteItem(item.id) }) {
            is Resource.Success -> {
                photoStorage.delete(item.imagePath) // FR-086
                itemDao.hardDelete(item.id)
                null
            }
            // Brisanje reda koji server vise ne zna je vec postignut cilj — tretira se kao uspeh.
            is Resource.Error -> if (result.code == ErrorCode.NOT_FOUND) discardLocally(item) else result
            Resource.Loading -> null
        }

    private suspend fun discardLocally(item: InventoryItemEntity): Resource.Error? {
        Log.i(TAG, "Server ne poznaje predmet ${item.id} — uklanjam ga lokalno")
        photoStorage.delete(item.imagePath) // FR-086
        itemDao.hardDelete(item.id)
        return null
    }

    // Delta preko `since` (FR-098) — vraca i tombstone redove, pa brisanje sa drugog uredjaja stigne i ovde.
    //
    // Delta je stranicena (tiket 28, blokirajuci nalaz 01): watermark se pamti POSLE SVAKE strane, a ne
    // jednom na kraju, i petlja se vrti dok server javlja `hasMore`. Ranije se cuvao `serverTime` posle
    // jedne strane od 500 redova — sve preko toga je klijent trajno preskakao.
    private suspend fun pull(): Resource<Unit> {
        var pages = 0

        while (pages < SYNC_MAX_PULL_PAGES) {
            val since = syncMetadataDao.get(SYNC_METADATA_ITEMS_LAST_SYNC_KEY)?.value

            when (val result = safeApiCall(errorMessageProvider) { api.getItems(since = since) }) {
                is Resource.Success -> {
                    val response = result.data

                    // Najstariji preskocen red na ovoj strani (vidi applyRemoteItem).
                    var oldestSkipped: Long? = null
                    response.items.forEach { dto ->
                        val skippedAt = applyRemoteItem(dto)
                        if (skippedAt != null) {
                            val current = oldestSkipped
                            oldestSkipped = if (current == null) skippedAt else minOf(current, skippedAt)
                        }
                    }

                    // Serversko vreme se cuva za sledeci delta pull, nikad System.currentTimeMillis() —
                    // sat na telefonu moze biti pogresan. Stariji backend ne salje nextSince, pa se pada
                    // na serverTime i ponasanje je isto kao pre tiketa 28.
                    val proposed = response.nextSince ?: response.serverTime
                    val watermark = capBelowSkipped(proposed, oldestSkipped)
                    syncMetadataDao.upsert(SyncMetadataEntity(SYNC_METADATA_ITEMS_LAST_SYNC_KEY, watermark))

                    if (watermark != proposed) {
                        // Watermark je zadrzan iza preskocenog reda; dalje stranicenje bi samo ponovo
                        // dovlacilo iste redove. Nastavlja se pri sledecem sync-u, kad push razresi
                        // lokalne izmene koje su preskakanje i izazvale.
                        return Resource.Success(Unit)
                    }
                    if (response.hasMore != true) return Resource.Success(Unit)
                }
                is Resource.Error -> return result
                Resource.Loading -> return Resource.Loading
            }

            pages++
        }

        // Odbrana od greske na serveru koja bi `hasMore` ostavila zauvek na true — sync sme da stane
        // nedovrsen, ali ne sme da se vrti u beskonacnoj petlji.
        Log.w(TAG, "Delta pull zaustavljen posle $SYNC_MAX_PULL_PAGES strana — sumnjivo ponasanje servera")
        return Resource.Success(Unit)
    }

    // Lokalna izmena koja jos nije poslata ima prednost nad serverskom verzijom dok se ne posalje —
    // pull je preskace umesto da ih prepise.
    //
    // Vraca `updatedAt` preskocenog reda (epoch millis) ili null ako je red primenjen. Pozivalac tim
    // podatkom spusta watermark ispod preskocenog reda: bez toga bi `since` presao preko njega i
    // istovremena serverska izmena tog istog predmeta nikad ne bi bila ponovo isporucena, jer delta
    // vraca samo ono sto je novije od watermark-a (tiket 28, nalaz 07).
    private suspend fun applyRemoteItem(dto: ItemDto): Long? {
        val local = itemDao.getById(dto.id)
        if (local != null && local.syncStatus != SyncStatus.SYNCED) {
            return DateMapper.isoToEpochMillisOrNull(dto.updatedAt)
        }

        // safeApiCall pokriva samo mrezni poziv; upis u Room je van njega. Neispravan ili neocekivan
        // red (strani kljuc na kategoriju/lokaciju koju Room jos ne zna, nedostajuce obavezno polje u
        // ItemMapper-u, nepoznata vrednost enum-a) je do sada izlazio u viewModelScope i rusio
        // aplikaciju. Jedan los red sme da bude preskocen, ali ne sme da obori ceo sync (nalaz 04).
        runCatching {
            if (dto.deletedAt != null) {
                photoStorage.delete(local?.imagePath)
                itemDao.hardDelete(dto.id)
            } else {
                // DB-RULE-02 (FR-085) — imagePath se ne prepisuje sa servera.
                itemDao.upsert(dto.toEntity(keepImagePath = local?.imagePath))
            }
        }.onFailure { error ->
            Log.e(TAG, "Predmet ${dto.id} sa servera nije mogao da se upise — preskacem red", error) // ERR-04
        }

        return null
    }

    // Watermark se spusta jednu milisekundu ispod najstarijeg preskocenog reda, da bi ga sledeci
    // delta pull (`updated_at > since`) ponovo obuhvatio.
    private fun capBelowSkipped(proposed: String, oldestSkippedMillis: Long?): String {
        val skipped = oldestSkippedMillis ?: return proposed
        val proposedMillis = runCatching { DateMapper.isoToEpochMillis(proposed) }.getOrNull() ?: return proposed
        if (proposedMillis < skipped) return proposed
        return DateMapper.epochMillisToIso(skipped - 1)
    }
}
