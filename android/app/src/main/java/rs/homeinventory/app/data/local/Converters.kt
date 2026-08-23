package rs.homeinventory.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun roleToString(v: UserRole): String = v.name
    @TypeConverter fun stringToRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun syncToString(v: SyncStatus): String = v.name
    @TypeConverter fun stringToSync(v: String): SyncStatus = SyncStatus.valueOf(v)
}
