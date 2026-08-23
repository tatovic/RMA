package rs.homeinventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import rs.homeinventory.app.data.local.entity.SyncMetadataEntity

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key")
    suspend fun get(key: String): SyncMetadataEntity?

    @Upsert suspend fun upsert(entry: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun clear()
}
