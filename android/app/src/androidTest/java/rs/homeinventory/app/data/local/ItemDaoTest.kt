package rs.homeinventory.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.LocationEntity

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var db: HomeInventoryDatabase
    private lateinit var itemDao: ItemDao

    private val userId = "user-1"
    private val otherUserId = "user-2"

    private val electronics = CategoryEntity(
        id = "cat-electronics", name = "Elektronika", description = null,
        iconKey = "ic_category_electronics", sortOrder = 1, updatedAt = 0L
    )
    private val furniture = CategoryEntity(
        id = "cat-furniture", name = "Namestaj", description = null,
        iconKey = "ic_category_furniture", sortOrder = 2, updatedAt = 0L
    )
    private val livingRoom = LocationEntity(
        id = "loc-living-room", userId = userId, name = "Dnevna soba", description = null,
        createdAt = 0L, updatedAt = 0L
    )
    private val kitchen = LocationEntity(
        id = "loc-kitchen", userId = userId, name = "Kuhinja", description = null,
        createdAt = 0L, updatedAt = 0L
    )

    @Before
    fun setUp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, HomeInventoryDatabase::class.java).build()
        itemDao = db.itemDao()

        db.categoryDao().upsertAll(listOf(electronics, furniture))
        db.locationDao().upsertAll(listOf(livingRoom, kitchen))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(
        id: String,
        name: String,
        manufacturer: String? = null,
        model: String? = null,
        serialNumber: String? = null,
        categoryId: String = electronics.id,
        locationId: String = livingRoom.id,
        currency: String = "RSD",
        estimatedValue: Long? = 1000L,
        quantity: Int = 1,
        deletedAt: Long? = null,
        userId: String = this.userId
    ) = InventoryItemEntity(
        id = id,
        userId = userId,
        name = name,
        description = null,
        categoryId = categoryId,
        locationId = locationId,
        manufacturer = manufacturer,
        model = model,
        serialNumber = serialNumber,
        quantity = quantity,
        purchasePrice = null,
        estimatedValue = estimatedValue,
        currency = currency,
        purchaseDate = null,
        warrantyExpirationDate = null,
        seller = null,
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
        deletedAt = deletedAt,
        imagePath = null,
        syncStatus = SyncStatus.SYNCED
    )

    @Test
    fun search_matchesAcrossAllSixFields() = runTest {
        itemDao.upsert(item(id = "1", name = "Samsung TV", manufacturer = "Samsung"))
        itemDao.upsert(item(id = "2", name = "Frizider", manufacturer = "Beko", model = "RCNE560"))
        itemDao.upsert(item(id = "3", name = "Fotelja", serialNumber = "SN-SAMS-99"))
        itemDao.upsert(
            item(
                id = "4", name = "Sto", categoryId = furniture.id
            )
        )
        itemDao.upsert(item(id = "5", name = "Lampa", locationId = kitchen.id))

        // pogodak po imenu
        assertEquals(listOf("1"), itemDao.search(userId, "samsung tv").first().map { it.id })

        // pogodak po proizvodjacu (i naziv i proizvodjac sadrze "samsung", ali OR nad istim redom ne duplira rezultat)
        assertEquals(listOf("1"), itemDao.search(userId, "samsung").first().map { it.id })

        // pogodak po modelu
        assertEquals(listOf("2"), itemDao.search(userId, "rcne560").first().map { it.id })

        // pogodak po serijskom broju
        assertEquals(listOf("3"), itemDao.search(userId, "sn-sams-99").first().map { it.id })

        // pogodak po nazivu kategorije
        assertEquals(listOf("4"), itemDao.search(userId, "namestaj").first().map { it.id })

        // pogodak po nazivu lokacije
        assertEquals(listOf("5"), itemDao.search(userId, "kuhinja").first().map { it.id })

        // prazan upit vraca sve aktivne predmete korisnika
        assertEquals(5, itemDao.search(userId, "").first().size)
    }

    @Test
    fun search_matchesSerbianDiacriticsFoldedToPlainLatin() = runTest {
        // upit stize vec normalizovan (SearchQueryNormalizer: trim + lowercase + sklonjena dijakritika,
        // isto mapiranje kao SR_FOLD_* u ItemDao), pa "sporet" mora da nadje "Šporet" (tiket 19).
        itemDao.upsert(item(id = "1", name = "Šporet", manufacturer = "Gorenje"))
        itemDao.upsert(item(id = "2", name = "Frižider", manufacturer = "Đorđević"))

        assertEquals(listOf("1"), itemDao.search(userId, "sporet").first().map { it.id })
        assertEquals(listOf("2"), itemDao.search(userId, "frizider").first().map { it.id })
        assertEquals(listOf("2"), itemDao.search(userId, "dordevic").first().map { it.id })
    }

    @Test
    fun search_excludesDeletedItems() = runTest {
        itemDao.upsert(item(id = "1", name = "Samsung TV", manufacturer = "Samsung"))
        itemDao.upsert(
            item(id = "2", name = "Samsung Monitor", manufacturer = "Samsung", deletedAt = 123L)
        )

        val result = itemDao.search(userId, "samsung").first()

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun observeAll_excludesDeletedAndOtherUsersItems() = runTest {
        itemDao.upsert(item(id = "1", name = "Aktivan predmet"))
        itemDao.upsert(item(id = "2", name = "Obrisan predmet", deletedAt = 999L))
        itemDao.upsert(item(id = "3", name = "Tudji predmet", userId = otherUserId))

        val result = itemDao.observeAll(userId).first()

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun categoryAggregates_groupByCategoryAndCurrency_andExcludeDeleted() = runTest {
        // dva RSD predmeta u istoj kategoriji - moraju se sabrati u jedan red
        itemDao.upsert(
            item(id = "1", name = "TV", categoryId = electronics.id, currency = "RSD", estimatedValue = 50_000L, quantity = 1)
        )
        itemDao.upsert(
            item(id = "2", name = "Slusalice", categoryId = electronics.id, currency = "RSD", estimatedValue = 5_000L, quantity = 2)
        )
        // isti kategorija, druga valuta - mora ostati poseban red (BR-009, ne sabira se sa RSD)
        itemDao.upsert(
            item(id = "3", name = "Laptop", categoryId = electronics.id, currency = "EUR", estimatedValue = 80_000L, quantity = 1)
        )
        // obrisan predmet se ne racuna u agregat
        itemDao.upsert(
            item(id = "4", name = "Stari monitor", categoryId = electronics.id, currency = "RSD", estimatedValue = 1_000_000L, deletedAt = 42L)
        )

        val aggregates = itemDao.observeCategoryAggregates(userId).first()

        val rsdRow = aggregates.single { it.categoryId == electronics.id && it.currency == "RSD" }
        assertEquals(3, rsdRow.itemCount)                  // 1 + 2
        assertEquals(60_000L, rsdRow.totalMinor)            // 50000 + 5000*2

        val eurRow = aggregates.single { it.categoryId == electronics.id && it.currency == "EUR" }
        assertEquals(1, eurRow.itemCount)
        assertEquals(80_000L, eurRow.totalMinor)

        assertTrue(aggregates.none { it.totalMinor == 1_000_000L })
    }

    // DB-RULE-03 (tiket 26) — predmet koji server nikad nije video ostaje PENDING_CREATE i posle
    // brisanja, da bi SyncManager mogao da ga obrise lokalno bez ijednog poziva serveru.
    @Test
    fun softDelete_keepsPendingCreateStatus_forNeverSyncedItem() = runTest {
        itemDao.upsert(item(id = "1", name = "Nov predmet").copy(syncStatus = SyncStatus.PENDING_CREATE))

        itemDao.softDelete("1", now = 500L)

        val stored = itemDao.getById("1")!!
        assertEquals(SyncStatus.PENDING_CREATE, stored.syncStatus)
        assertEquals(500L, stored.deletedAt)
    }

    @Test
    fun softDelete_setsPendingDelete_forAlreadySyncedItem() = runTest {
        itemDao.upsert(item(id = "1", name = "Sinhronizovan predmet"))

        itemDao.softDelete("1", now = 500L)

        assertEquals(SyncStatus.PENDING_DELETE, itemDao.getById("1")!!.syncStatus)
    }

    // FR-027 opoziv brisanja mora vratiti isti status koji je predmet imao pre brisanja — inace bi
    // sync pokusao PUT nad predmetom koji server nikad nije video.
    @Test
    fun undoDelete_restoresPendingCreateStatus_forNeverSyncedItem() = runTest {
        itemDao.upsert(item(id = "1", name = "Nov predmet").copy(syncStatus = SyncStatus.PENDING_CREATE))
        itemDao.softDelete("1", now = 500L)

        itemDao.undoDelete("1")

        val stored = itemDao.getById("1")!!
        assertEquals(SyncStatus.PENDING_CREATE, stored.syncStatus)
        assertEquals(null, stored.deletedAt)
    }

    @Test
    fun undoDelete_setsPendingUpdate_forPreviouslySyncedItem() = runTest {
        itemDao.upsert(item(id = "1", name = "Sinhronizovan predmet"))
        itemDao.softDelete("1", now = 500L)

        itemDao.undoDelete("1")

        assertEquals(SyncStatus.PENDING_UPDATE, itemDao.getById("1")!!.syncStatus)
    }

    @Test
    fun clearAllData_removesContentFromEveryTable() = runTest {
        itemDao.upsert(item(id = "1", name = "Predmet"))

        db.clearAllData()

        assertTrue(itemDao.observeAll(userId).first().isEmpty())
        assertTrue(db.categoryDao().getAll().isEmpty())
        assertTrue(db.locationDao().observeAll(userId).first().isEmpty())
    }

    // ---- Tiket 28 ----

    // DB-RULE-04 (blokirajuci nalaz 03) — `updatedAt` je verzija koju je izdao SERVER i lokalno
    // brisanje je ne sme prepisati satom sa telefona. Ranije je softDelete upisivao `now` i u
    // updatedAt, pa je uredjaj sa zaostalim satom slao verziju stariju od serverske.
    @Test
    fun softDelete_doesNotOverwriteServerIssuedUpdatedAt() = runTest {
        itemDao.upsert(item(id = "1", name = "Predmet").copy(updatedAt = 12_345L))

        itemDao.softDelete("1", now = 999_999L)

        val stored = itemDao.getById("1")!!
        assertEquals(12_345L, stored.updatedAt)
        assertEquals(999_999L, stored.deletedAt)
    }

    @Test
    fun undoDelete_doesNotOverwriteServerIssuedUpdatedAt() = runTest {
        itemDao.upsert(item(id = "1", name = "Predmet").copy(updatedAt = 12_345L))
        itemDao.softDelete("1", now = 999_999L)

        itemDao.undoDelete("1")

        assertEquals(12_345L, itemDao.getById("1")!!.updatedAt)
    }

    // Blokirajuci nalaz 04 — soft-obrisan predmet je i dalje red koji strani kljuc drzi, pa bi
    // brisanje lokacije palo na SQLiteConstraintException. Tombstone redovi se zato uklanjaju rucno
    // neposredno pre roditelja (ItemRepository.deleteLocation).
    @Test
    fun hardDeleteByLocation_removesTombstonesSoParentCanBeDeleted() = runTest {
        itemDao.upsert(item(id = "1", name = "Obrisan", locationId = kitchen.id, deletedAt = 500L))
        itemDao.upsert(item(id = "2", name = "Drugde", locationId = livingRoom.id))

        itemDao.hardDeleteByLocation(kitchen.id)
        db.locationDao().hardDelete(kitchen.id)

        assertEquals(null, itemDao.getById("1"))
        assertEquals("2", itemDao.getById("2")!!.id)
    }

    @Test
    fun hardDeleteByCategory_removesTombstonesSoParentCanBeDeleted() = runTest {
        itemDao.upsert(item(id = "1", name = "Obrisan", categoryId = furniture.id, deletedAt = 500L))
        itemDao.upsert(item(id = "2", name = "Drugde", categoryId = electronics.id))

        itemDao.hardDeleteByCategory(furniture.id)
        db.categoryDao().delete(furniture.id)

        assertEquals(null, itemDao.getById("1"))
        assertEquals("2", itemDao.getById("2")!!.id)
    }

    // BR-005 (nalaz 10) — istekla sesija cuva neposlat rad prijavljenog korisnika, za razliku od
    // odjave koja brise sve. Tudji redovi i sinhronizovan kes odlaze.
    @Test
    fun clearPreservingUnsyncedWork_keepsOnlyOwnPendingRows() = runTest {
        itemDao.upsert(item(id = "synced", name = "Sa servera"))
        itemDao.upsert(item(id = "pending", name = "Neposlat").copy(syncStatus = SyncStatus.PENDING_CREATE))
        itemDao.upsert(
            item(id = "tudji", name = "Tudji neposlat", userId = otherUserId)
                .copy(syncStatus = SyncStatus.PENDING_UPDATE)
        )

        db.clearPreservingUnsyncedWork(userId)

        assertEquals(null, itemDao.getById("synced"))
        assertEquals(null, itemDao.getById("tudji"))
        assertEquals("Neposlat", itemDao.getById("pending")!!.name)
    }

    // Kategorija/lokacija koju preostali red jos drzi mora prezivati ciscenje — inace bi brisanje
    // roditelja palo na strani kljuc, a predmet ostao bez naziva kategorije i lokacije.
    @Test
    fun clearPreservingUnsyncedWork_keepsParentsOfPreservedRows() = runTest {
        itemDao.upsert(
            item(id = "pending", name = "Neposlat", categoryId = electronics.id, locationId = livingRoom.id)
                .copy(syncStatus = SyncStatus.PENDING_CREATE)
        )

        db.clearPreservingUnsyncedWork(userId)

        assertEquals(listOf(electronics.id), db.categoryDao().getAll().map { it.id })
        assertEquals(listOf(livingRoom.id), db.locationDao().observeAll(userId).first().map { it.id })
    }

    @Test
    fun clearPreservingUnsyncedWork_withoutUserClearsEverything() = runTest {
        itemDao.upsert(item(id = "pending", name = "Neposlat").copy(syncStatus = SyncStatus.PENDING_CREATE))

        db.clearPreservingUnsyncedWork(null)

        assertEquals(null, itemDao.getById("pending"))
        assertTrue(db.categoryDao().getAll().isEmpty())
    }

    // OWN-01/OWN-07 — push sme da posalje samo redove prijavljenog korisnika.
    @Test
    fun getPending_returnsOnlyRowsOfGivenUser() = runTest {
        itemDao.upsert(item(id = "moj", name = "Moj").copy(syncStatus = SyncStatus.PENDING_CREATE))
        itemDao.upsert(
            item(id = "tudji", name = "Tudji", userId = otherUserId).copy(syncStatus = SyncStatus.PENDING_CREATE)
        )
        itemDao.upsert(item(id = "sinhronizovan", name = "Sinhronizovan"))

        assertEquals(listOf("moj"), itemDao.getPending(userId).map { it.id })
    }

    // Nalaz C7 — `%` i `_` iz korisnickog unosa su LIKE dzokeri; upit koji ih sadrzi sme da nadje
    // samo doslovno poklapanje. Upit stize normalizovan (SearchQueryNormalizer ih vec escapuje).
    @Test
    fun search_treatsWildcardCharactersLiterally() = runTest {
        itemDao.upsert(item(id = "1", name = "Popust 50%"))
        itemDao.upsert(item(id = "2", name = "Obican predmet"))

        val hits = itemDao.search(userId, "50\\%").first().map { it.name }

        assertEquals(listOf("Popust 50%"), hits)
    }
}
