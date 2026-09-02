package rs.homeinventory.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
}
