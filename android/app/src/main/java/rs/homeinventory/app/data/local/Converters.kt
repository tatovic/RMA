package rs.homeinventory.app.data.local

import androidx.room.TypeConverter

// ERR-02 duh (tiket 28, nalaz C10) — `valueOf` baca IllegalArgumentException na svaku vrednost koju
// enum ne poznaje. Ovde bi to znacilo rusenje aplikacije pri obicnom citanju iz baze: dovoljno je da
// server jednog dana uvede novu rolu ili da red ostane iz starije verzije aplikacije. Nepoznata
// vrednost zato pada na najmanje stetnu podrazumevanu — obicnog korisnika, odnosno "sinhronizovano".
class Converters {
    @TypeConverter fun roleToString(v: UserRole): String = v.name

    @TypeConverter
    fun stringToRole(v: String): UserRole =
        UserRole.entries.find { it.name == v } ?: UserRole.USER

    @TypeConverter fun syncToString(v: SyncStatus): String = v.name

    // SYNCED je bezbedan izbor: u najgorem slucaju red ceka sledecu izmenu da bi bio poslat, dok bi
    // pogadjanje nekog PENDING statusa moglo da posalje serveru nesto sto korisnik nije trazio.
    @TypeConverter
    fun stringToSync(v: String): SyncStatus =
        SyncStatus.entries.find { it.name == v } ?: SyncStatus.SYNCED
}
