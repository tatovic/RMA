package rs.homeinventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.entity.LocationEntity

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE userId = :userId AND syncStatus != 'PENDING_DELETE' ORDER BY name ASC")
    fun observeAll(userId: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: String): LocationEntity?

    @Upsert suspend fun upsert(location: LocationEntity)
    @Upsert suspend fun upsertAll(locations: List<LocationEntity>)

    @Query("UPDATE locations SET syncStatus = :status WHERE id = :id")
    suspend fun setSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT * FROM locations WHERE syncStatus != 'SYNCED'")
    suspend fun getPending(): List<LocationEntity>

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM locations")
    suspend fun clear()
}
