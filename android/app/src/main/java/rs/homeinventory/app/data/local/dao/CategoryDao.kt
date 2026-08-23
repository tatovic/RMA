package rs.homeinventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.homeinventory.app.data.local.entity.CategoryEntity

// Kes globalnih kategorija sa servera - server pise, klijent samo cita.
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Upsert suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clear()
}
