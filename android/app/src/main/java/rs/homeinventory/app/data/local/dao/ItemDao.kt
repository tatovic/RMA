package rs.homeinventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.entity.InventoryItemEntity

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
               c.id AS categoryId, c.name AS categoryName, c.iconKey AS categoryIconKey,
               l.id AS locationId, l.name AS locationName
        FROM inventory_items i
        JOIN categories c ON c.id = i.categoryId
        JOIN locations  l ON l.id = i.locationId
        WHERE i.userId = :userId
          AND i.deletedAt IS NULL
          AND i.syncStatus != 'PENDING_DELETE'
        ORDER BY i.createdAt DESC
        """
    )
    fun observeAll(userId: String): Flow<List<ItemListRow>>

    // ---- Pretraga po SEST polja (FR-031) ----
    // :query mora stici vec normalizovan (trim + lowercase + dijakritike svedene na osnovna slova,
    // vidi SearchQueryNormalizer, tech.md 8.5). SQLite LOWER()/COLLATE NOCASE ne pokrivaju srpske
    // dijakritike (Č, Ć, Š, Ž, Đ), pa se i sama kolona istim REPLACE lancem svodi na golu latinicu
    // pre poredjenja — tako upit "sporet" nalazi "Šporet" (tiket 19).
    @Query(
        """
        SELECT i.id, i.name, i.manufacturer, i.model, i.quantity,
               i.purchasePrice, i.estimatedValue, i.currency,
               i.purchaseDate, i.warrantyExpirationDate, i.imagePath,
               i.createdAt, i.syncStatus,
               c.id AS categoryId, c.name AS categoryName, c.iconKey AS categoryIconKey,
               l.id AS locationId, l.name AS locationName
        FROM inventory_items i
        JOIN categories c ON c.id = i.categoryId
        JOIN locations  l ON l.id = i.locationId
        WHERE i.userId = :userId
          AND i.deletedAt IS NULL
          AND i.syncStatus != 'PENDING_DELETE'
          AND (:query = '' OR
               $SR_FOLD_PREFIX i.name         $SR_FOLD_SUFFIX LIKE '%' || :query || '%' OR
               $SR_FOLD_PREFIX i.manufacturer $SR_FOLD_SUFFIX LIKE '%' || :query || '%' OR
               $SR_FOLD_PREFIX i.model        $SR_FOLD_SUFFIX LIKE '%' || :query || '%' OR
               $SR_FOLD_PREFIX i.serialNumber $SR_FOLD_SUFFIX LIKE '%' || :query || '%' OR
               $SR_FOLD_PREFIX c.name         $SR_FOLD_SUFFIX LIKE '%' || :query || '%' OR
               $SR_FOLD_PREFIX l.name         $SR_FOLD_SUFFIX LIKE '%' || :query || '%')
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
               c.name AS categoryName, l.name AS locationName
        FROM inventory_items i
        JOIN categories c ON c.id = i.categoryId
        JOIN locations  l ON l.id = i.locationId
        WHERE i.id = :id AND i.deletedAt IS NULL
        """
    )
    fun observeDetails(id: String): Flow<ItemDetailsRow?>

    // ---- Poslednjih N dodatih predmeta (SCR-03) ----
    @Query(
        """
        SELECT i.id, i.name, i.manufacturer, i.model, i.quantity,
               i.purchasePrice, i.estimatedValue, i.currency,
               i.purchaseDate, i.warrantyExpirationDate, i.imagePath,
               i.createdAt, i.syncStatus,
               c.id AS categoryId, c.name AS categoryName, c.iconKey AS categoryIconKey,
               l.id AS locationId, l.name AS locationName
        FROM inventory_items i
        JOIN categories c ON c.id = i.categoryId
        JOIN locations  l ON l.id = i.locationId
        WHERE i.userId = :userId
          AND i.deletedAt IS NULL
          AND i.syncStatus != 'PENDING_DELETE'
        ORDER BY i.createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecent(userId: String, limit: Int): Flow<List<ItemListRow>>

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
    @Query("SELECT * FROM inventory_items WHERE syncStatus != 'SYNCED'")
    suspend fun getPending(): List<InventoryItemEntity>

    @Upsert suspend fun upsert(item: InventoryItemEntity)
    @Upsert suspend fun upsertAll(items: List<InventoryItemEntity>)

    @Query("UPDATE inventory_items SET syncStatus = :status WHERE id = :id")
    suspend fun setSyncStatus(id: String, status: SyncStatus)

    // Soft delete (FR-026)
    @Query("UPDATE inventory_items SET deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    // Opoziv brisanja u roku od pet sekundi (FR-027) — puna sinhronizacija sa serverom dolazi u tiketu 26.
    @Query("UPDATE inventory_items SET deletedAt = NULL, syncStatus = 'PENDING_UPDATE' WHERE id = :id")
    suspend fun undoDelete(id: String)

    // Fizicko uklanjanje tek POSLE potvrde servera
    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM inventory_items")
    suspend fun clear()
}
