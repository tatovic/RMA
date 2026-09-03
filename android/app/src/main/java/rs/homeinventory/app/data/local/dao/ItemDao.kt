package rs.homeinventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.entity.InventoryItemEntity

// C6 (tiket 28) — svi upiti za listu koriste LEFT JOIN, ne INNER JOIN. Sa INNER JOIN-om je predmet
// cija kategorija ili lokacija nedostaje u Room-u TIHO nestajao iz liste: korisnik bi video da mu
// predmet fali, bez ijedne poruke i bez ikakvog traga u logu. Sa LEFT JOIN-om predmet ostaje vidljiv
// i samo izgleda cudno, sto je stanje iz kojeg korisnik moze da se oporavi (povlacenjem liste nadole).
// Zamenski naziv je tipografski znak, ne recenica — NFR-09 se odnosi na tekst, ne na crticu.
private const val MISSING_PARENT_NAME = "—"

// Svodi kolonu na malo slovo bez srpskih dijakritika (Š,Č,Ć,Ž,Đ -> s,c,c,z,d), isti mapping kao
// SearchQueryNormalizer na strani upita — samo tako SQL LIKE moze da poredi "sporet" sa "Šporet".
private const val SR_FOLD_PREFIX =
    "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LOWER("
private const val SR_FOLD_SUFFIX =
    "),'š','s'),'č','c'),'ć','c'),'ž','z'),'đ','d'),'Š','s'),'Č','c'),'Ć','c'),'Ž','z'),'Đ','d')"

@Dao
interface ItemDao {

    // ---- Citanje ----
    @Query(
        """
        SELECT i.id, i.name, i.manufacturer, i.model, i.quantity,
               i.purchasePrice, i.estimatedValue, i.currency,
               i.purchaseDate, i.warrantyExpirationDate, i.imagePath,
               i.createdAt, i.syncStatus,
               i.categoryId AS categoryId, COALESCE(c.name, '$MISSING_PARENT_NAME') AS categoryName,
               c.iconKey AS categoryIconKey,
               i.locationId AS locationId, COALESCE(l.name, '$MISSING_PARENT_NAME') AS locationName
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        LEFT JOIN locations  l ON l.id = i.locationId
        WHERE i.userId = :userId
          AND i.deletedAt IS NULL
          AND i.syncStatus != 'PENDING_DELETE'
        ORDER BY i.createdAt DESC
        """
    )
    fun observeAll(userId: String): Flow<List<ItemListRow>>

    // Odgovara samo na pitanje "ima li korisnik ijedan predmet" (razlika izmedju praznog inventara i
    // pretrage bez rezultata). Ranije je isti odgovor dobijan kroz jos jedan observeAll(), pa je svaka
    // izmena u bazi pokretala dva identicna upita nad celom listom i dva mapiranja u ItemListRow
    // (tiket 28, nalaz C8).
    @Query(
        """
        SELECT COUNT(*) FROM inventory_items
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND syncStatus != 'PENDING_DELETE'
        """
    )
    fun observeCount(userId: String): Flow<Int>

    // ---- Pretraga po SEST polja (FR-031) ----
    // :query mora stici vec normalizovan (trim + lowercase + dijakritike svedene na osnovna slova,
    // vidi SearchQueryNormalizer, tech.md 8.5). SQLite LOWER()/COLLATE NOCASE ne pokrivaju srpske
    // dijakritike (Č, Ć, Š, Ž, Đ), pa se i sama kolona istim REPLACE lancem svodi na golu latinicu
    // pre poredjenja — tako upit "sporet" nalazi "Šporet" (tiket 19).
    //
    // ESCAPE '\' uz svaki LIKE (tiket 28, nalaz C7): bez njega su `%` i `_` iz korisnickog unosa
    // bili LIKE dzokeri, pa je upit "50%" vracao sve, a "a_b" i "axb". SearchQueryNormalizer sada
    // ispred `%`, `_` i samog `\` stavlja obrnutu kosu crtu, a ovde se ta crta proglasava escape
    // znakom. Oba kraja moraju da se poklapaju — izmena jednog bez drugog kvari pretragu.
    //
    // Poznata cena koja OSTAJE: REPLACE lanac se izvrsava nad svakim redom i sprecava koriscenje
    // indeksa. Pravo resenje je perzistirana "svedena" kolona, sto trazi izmenu Room seme koju ovaj
    // tiket namerno izbegava — zabelezeno kao naredni korak u docs/tickets/28.
    @Query(
        """
        SELECT i.id, i.name, i.manufacturer, i.model, i.quantity,
               i.purchasePrice, i.estimatedValue, i.currency,
               i.purchaseDate, i.warrantyExpirationDate, i.imagePath,
               i.createdAt, i.syncStatus,
               i.categoryId AS categoryId, COALESCE(c.name, '$MISSING_PARENT_NAME') AS categoryName,
               c.iconKey AS categoryIconKey,
               i.locationId AS locationId, COALESCE(l.name, '$MISSING_PARENT_NAME') AS locationName
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        LEFT JOIN locations  l ON l.id = i.locationId
        WHERE i.userId = :userId
          AND i.deletedAt IS NULL
          AND i.syncStatus != 'PENDING_DELETE'
          AND (:query = '' OR
               $SR_FOLD_PREFIX i.name         $SR_FOLD_SUFFIX LIKE '%' || :query || '%' ESCAPE '\' OR
               $SR_FOLD_PREFIX i.manufacturer $SR_FOLD_SUFFIX LIKE '%' || :query || '%' ESCAPE '\' OR
               $SR_FOLD_PREFIX i.model        $SR_FOLD_SUFFIX LIKE '%' || :query || '%' ESCAPE '\' OR
               $SR_FOLD_PREFIX i.serialNumber $SR_FOLD_SUFFIX LIKE '%' || :query || '%' ESCAPE '\' OR
               $SR_FOLD_PREFIX c.name         $SR_FOLD_SUFFIX LIKE '%' || :query || '%' ESCAPE '\' OR
               $SR_FOLD_PREFIX l.name         $SR_FOLD_SUFFIX LIKE '%' || :query || '%' ESCAPE '\')
        ORDER BY i.createdAt DESC
        """
    )
    fun search(userId: String, query: String): Flow<List<ItemListRow>>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<InventoryItemEntity?>

    // Koristi se pri punjenju iz mreze da bi se sacuvao lokalni imagePath (DB-RULE-02) - namerno bez deletedAt filtera.
    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: String): InventoryItemEntity?

    // ---- Detalji predmeta (SCR-07, tiket 16) — BR-007: samo id se prosledjuje ekranu, on sam ucitava iz Room-a. ----
    @Query(
        """
        SELECT i.id, i.name, i.description, i.manufacturer, i.model, i.serialNumber,
               i.quantity, i.purchasePrice, i.estimatedValue, i.currency,
               i.purchaseDate, i.warrantyExpirationDate, i.seller, i.notes, i.imagePath,
               COALESCE(c.name, '$MISSING_PARENT_NAME') AS categoryName,
               COALESCE(l.name, '$MISSING_PARENT_NAME') AS locationName
        FROM inventory_items i
        LEFT JOIN categories c ON c.id = i.categoryId
        LEFT JOIN locations  l ON l.id = i.locationId
        WHERE i.id = :id AND i.deletedAt IS NULL
        """
    )
    fun observeDetails(id: String): Flow<ItemDetailsRow?>

    // ---- Garancije (FR-053), sortirano po hitnosti - najskoriji datum prvi ----
    @Query(
        """
        SELECT * FROM inventory_items
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND warrantyExpirationDate IS NOT NULL
          AND warrantyExpirationDate >= :today
          AND warrantyExpirationDate <= :thresholdDate
        ORDER BY warrantyExpirationDate ASC
        """
    )
    fun observeExpiringWarranties(
        userId: String, today: String, thresholdDate: String
    ): Flow<List<InventoryItemEntity>>

    // ---- Agregacija po kategoriji I valuti (BR-009) ----
    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, i.currency AS currency,
               SUM(i.quantity) AS itemCount,
               SUM(COALESCE(i.estimatedValue, i.purchasePrice, 0) * i.quantity) AS totalMinor
        FROM inventory_items i
        JOIN categories c ON c.id = i.categoryId
        WHERE i.userId = :userId AND i.deletedAt IS NULL
        GROUP BY c.id, c.name, i.currency
        """
    )
    fun observeCategoryAggregates(userId: String): Flow<List<CategoryAggregate>>

    // ---- Provere pre brisanja (BR-014) ----
    @Query("SELECT COUNT(*) FROM inventory_items WHERE categoryId = :id AND deletedAt IS NULL")
    suspend fun countByCategory(id: String): Int

    @Query("SELECT COUNT(*) FROM inventory_items WHERE locationId = :id AND deletedAt IS NULL")
    suspend fun countByLocation(id: String): Int

    // ---- Sinhronizacija ----
    // OWN-01/OWN-07 — push salje samo redove prijavljenog korisnika. Otkad istekla sesija cuva
    // neposlat rad (tiket 28, nalaz 10), u bazi mogu kratko postojati redovi prethodnog naloga; bez
    // ovog filtera bi ih sledeca prijava poslala tudjim tokenom.
    @Query("SELECT * FROM inventory_items WHERE syncStatus != 'SYNCED' AND userId = :userId")
    suspend fun getPending(userId: String): List<InventoryItemEntity>

    @Upsert suspend fun upsert(item: InventoryItemEntity)
    @Upsert suspend fun upsertAll(items: List<InventoryItemEntity>)

    @Query("UPDATE inventory_items SET syncStatus = :status WHERE id = :id")
    suspend fun setSyncStatus(id: String, status: SyncStatus)

    // Soft delete (FR-026). PENDING_CREATE se namerno NE prebacuje u PENDING_DELETE (DB-RULE-03) —
    // server jos ne zna za predmet, pa SyncManager mora moci da prepozna taj slucaj i obrise ga
    // lokalno bez ijednog poziva servera. Svi ostali statusi idu u PENDING_DELETE kao i do sada.
    //
    // DB-RULE-04 — `updatedAt` se NE dira. To nije vreme lokalne izmene nego poslednja verzija koju
    // je server izdao za taj red; upisivanje sata sa telefona preko nje je klijentu sa zaostalim
    // satom donosilo tihi gubitak izmena (tiket 28, blokirajuci nalaz 03). `deletedAt` je lokalna
    // oznaka i ostaje.
    @Query(
        """
        UPDATE inventory_items
        SET deletedAt = :now,
            syncStatus = CASE WHEN syncStatus = 'PENDING_CREATE' THEN 'PENDING_CREATE' ELSE 'PENDING_DELETE' END
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: String, now: Long)

    // Opoziv brisanja u roku od pet sekundi (FR-027). Isto pravilo kao kod softDelete: predmet koji
    // server nikad nije video ostaje PENDING_CREATE (ceka POST), ne PENDING_UPDATE (koje bi pokusalo
    // PUT nad predmetom koji server ne poznaje). `updatedAt` se ne dira iz istog razloga kao gore
    // (DB-RULE-04, tiket 28).
    @Query(
        """
        UPDATE inventory_items
        SET deletedAt = NULL,
            syncStatus = CASE WHEN syncStatus = 'PENDING_CREATE' THEN 'PENDING_CREATE' ELSE 'PENDING_UPDATE' END
        WHERE id = :id
        """
    )
    suspend fun undoDelete(id: String)

    // Fizicko uklanjanje tek POSLE potvrde servera
    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun hardDelete(id: String)

    // Room strani kljucevi ka categories/locations su NO_ACTION, pa tombstone red (soft-obrisan
    // predmet koji korisnik vise ne vidi) i dalje drzi roditelja i obara brisanje lokacije/kategorije
    // sa SQLiteConstraintException. Server isti problem resava kaskadom (migracija 001); ovde se
    // tombstone redovi brisu rucno, tacno pre roditelja, cime se izbegava izmena Room seme
    // (tiket 28, blokirajuci nalaz 04).
    @Query("DELETE FROM inventory_items WHERE locationId = :locationId")
    suspend fun hardDeleteByLocation(locationId: String)

    @Query("DELETE FROM inventory_items WHERE categoryId = :categoryId")
    suspend fun hardDeleteByCategory(categoryId: String)

    @Query("DELETE FROM inventory_items")
    suspend fun clear()

    // BR-005 (tiket 28, nalaz 10) — istekla sesija cuva NEPOSLAT rad prijavljenog korisnika, za razliku
    // od odjave koja brise sve. Ostaju samo redovi koji su istovremeno njegovi i jos nesinhronizovani;
    // sve ostalo je kes koji server moze da vrati.
    @Query("DELETE FROM inventory_items WHERE syncStatus = 'SYNCED' OR userId != :userId")
    suspend fun clearExceptUnsyncedOf(userId: String)

    // OWN-07 — prijava drugog naloga na istom uredjaju ne sme da zatekne tudje redove sacuvane
    // prethodnom istekom sesije.
    @Query("DELETE FROM inventory_items WHERE userId != :userId")
    suspend fun clearOtherUsers(userId: String)
}
