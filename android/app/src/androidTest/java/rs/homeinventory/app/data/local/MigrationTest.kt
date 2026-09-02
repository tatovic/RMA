package rs.homeinventory.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

// Tiket 27 — dokazuje da MIGRATION_1_2 stvarno radi na postojecoj (v1) bazi, bez brisanja podataka
// (za razliku od fallbackToDestructiveMigration, koji ovaj projekat namerno ne koristi nigde).
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HomeInventoryDatabase::class.java
    )

    @Test
    fun migrate1To2_cuvaPostojecePredmeteIDodajeIndeksPotrebanZaListuOd500Predmeta() {
        // v1 baza sa jednim vec sacuvanim predmetom — simulira uredjaj koji dobija azuriranje aplikacije.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO categories (id, name, description, iconKey, sortOrder, updatedAt) " +
                    "VALUES ('cat-1', 'Elektronika', NULL, NULL, 1, 0)"
            )
            execSQL(
                "INSERT INTO locations (id, userId, name, description, createdAt, updatedAt, syncStatus) " +
                    "VALUES ('loc-1', 'user-1', 'Dnevna soba', NULL, 0, 0, 'SYNCED')"
            )
            execSQL(
                "INSERT INTO inventory_items (id, userId, name, description, categoryId, locationId, " +
                    "manufacturer, model, serialNumber, quantity, purchasePrice, estimatedValue, currency, " +
                    "purchaseDate, warrantyExpirationDate, seller, notes, createdAt, updatedAt, deletedAt, " +
                    "imagePath, syncStatus) VALUES ('item-1', 'user-1', 'Televizor', NULL, 'cat-1', 'loc-1', " +
                    "NULL, NULL, NULL, 1, NULL, NULL, 'RSD', NULL, NULL, NULL, NULL, 1000, 1000, NULL, NULL, " +
                    "'SYNCED')"
            )
            close()
        }

        // Prava migracija (ne fallbackToDestructiveMigration) — validate=true proverava da je sema
        // posle migracije identicna onoj koju Room ocekuje iz trenutnih Entity klasa (2.json).
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val row = migratedDb.query("SELECT name FROM inventory_items WHERE id = 'item-1'")
        assertTrue("Predmet sacuvan pre migracije mora preziveti migraciju bez gubitka podataka", row.moveToFirst())
        assertEquals("Televizor", row.getString(0))
        row.close()

        val index = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' " +
                "AND name = 'index_inventory_items_userId_createdAt'"
        )
        assertTrue("MIGRATION_1_2 mora da doda indeks (userId, createdAt), NFR-01", index.moveToFirst())
        index.close()

        migratedDb.close()
    }
}
