package rs.homeinventory.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Tiket 27 — prava migracija umesto razarajuce (fallbackToDestructiveMigration se ovde nikad nije
// koristio, ali baza dotad nije imala nijednu Migration ni fallback strategiju, pa bi svaka buduca
// promena seme srusila aplikaciju pri sledecem pokretanju; MIGRATION_1_2 je prvi realan primer i
// pokriven je MigrationTest-om). Dodaje indeks (userId, createdAt) koji pokriva WHERE+ORDER BY iz
// svakog upita liste u ItemDao (NFR-01 — lista mora ostati responzivna sa najmanje 500 predmeta).
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_inventory_items_userId_createdAt` " +
                "ON `inventory_items` (`userId`, `createdAt`)"
        )
    }
}
