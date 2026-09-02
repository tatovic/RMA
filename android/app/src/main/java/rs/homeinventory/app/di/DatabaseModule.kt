package rs.homeinventory.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.homeinventory.app.data.local.HomeInventoryDatabase
import rs.homeinventory.app.data.local.MIGRATION_1_2
import rs.homeinventory.app.data.local.dao.CategoryDao
import rs.homeinventory.app.data.local.dao.ExchangeRateDao
import rs.homeinventory.app.data.local.dao.ItemDao
import rs.homeinventory.app.data.local.dao.LocationDao
import rs.homeinventory.app.data.local.dao.SyncMetadataDao
import rs.homeinventory.app.data.local.dao.UserDao
import javax.inject.Singleton

// tech.md sekcija 9.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): HomeInventoryDatabase =
        Room.databaseBuilder(context, HomeInventoryDatabase::class.java, "home_inventory.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun userDao(database: HomeInventoryDatabase): UserDao = database.userDao()

    @Provides
    fun categoryDao(database: HomeInventoryDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun locationDao(database: HomeInventoryDatabase): LocationDao = database.locationDao()

    @Provides
    fun itemDao(database: HomeInventoryDatabase): ItemDao = database.itemDao()

    @Provides
    fun exchangeRateDao(database: HomeInventoryDatabase): ExchangeRateDao = database.exchangeRateDao()

    @Provides
    fun syncMetadataDao(database: HomeInventoryDatabase): SyncMetadataDao = database.syncMetadataDao()
}
