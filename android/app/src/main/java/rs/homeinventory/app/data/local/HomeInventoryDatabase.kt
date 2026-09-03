package rs.homeinventory.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.homeinventory.app.data.local.dao.CategoryDao
import rs.homeinventory.app.data.local.dao.ExchangeRateDao
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.dao.LocationDao
import rs.homeinventory.app.data.local.dao.SyncMetadataDao
import rs.homeinventory.app.data.local.dao.UserDao
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.ExchangeRateEntity
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.data.local.entity.SyncMetadataEntity
import rs.homeinventory.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        LocationEntity::class,
        InventoryItemEntity::class,
        ExchangeRateEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HomeInventoryDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    // Brise kompletan sadrzaj baze, za potrebe odjave (BR-005, OWN-07).
    suspend fun clearAllData() = withContext(Dispatchers.IO) { clearAllTables() }

    // BR-005, druga polovina (tiket 28, nalaz 10) — istekla sesija NIJE odjava. Korisnik nije tražio
    // da ode; server je samo prestao da priznaje token. Brisanje cele baze je u tom trenutku uništavalo
    // predmete koje offline rad (FR-097) namerno drži lokalno dok ne budu poslati — jedini primerak
    // tog unosa. Ostaje isključivo neposlat rad TOG korisnika, uz kategorije i lokacije bez kojih ti
    // redovi ne mogu da postoje (strani ključ je NO_ACTION).
    //
    // `userId == null` znači da se ne zna čiji je rad (sesija bez zapamćenog korisnika) — tada nema
    // čemu da se da prednost i briše se sve, kao kod odjave.
    suspend fun clearPreservingUnsyncedWork(userId: String?) = withContext(Dispatchers.IO) {
        if (userId == null) {
            clearAllTables()
            return@withContext
        }

        withTransaction {
            itemDao().clearExceptUnsyncedOf(userId)
            locationDao().clearUnreferenced()
            categoryDao().clearUnreferenced()
            userDao().clear()
            exchangeRateDao().clear()
            // Watermark pripada prošloj sesiji — briše se da sledeći pull krene od nule. Pun pull
            // (bez `since`) preskače redove koji čekaju slanje, pa im ne može ništa (SyncManager).
            syncMetadataDao().clear()
        }
    }
}
