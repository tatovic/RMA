package rs.homeinventory.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.homeinventory.app.data.local.HomeInventoryDatabase
import rs.homeinventory.app.data.local.dao.UserDao
import javax.inject.Singleton

// tech.md sekcija 9.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): HomeInventoryDatabase =
        Room.databaseBuilder(context, HomeInventoryDatabase::class.java, "home_inventory.db").build()

    @Provides
    fun userDao(database: HomeInventoryDatabase): UserDao = database.userDao()
}
