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

    // SCR-10 — spisak lokacija sa brojem predmeta uz svaku (FR-049, tiket 17).
    @Query(
        """
        SELECT l.id AS id, l.name AS name, l.description AS description,
               COUNT(i.id) AS itemCount
        FROM locations l
        LEFT JOIN inventory_items i ON i.locationId = l.id AND i.deletedAt IS NULL
        WHERE l.userId = :userId AND l.syncStatus != 'PENDING_DELETE'
        GROUP BY l.id
        ORDER BY l.name ASC
        """
    )
    fun observeAllWithItemCount(userId: String): Flow<List<LocationWithCount>>

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
