# Home Inventory — Model podataka (DB)

| Polje | Vrednost |
|---|---|
| Verzija dokumenta | 1.0 |
| Datum | 2026-08-22 |
| Status | Odobreno — osnova za implementaciju |
| Prateći dokumenti | `prd.md` (zahtevi), `tech.md` (arhitektura) |
| Backend baza | MySQL 8.0 (već instalirana, servis `MySQL80`) |
| Lokalna baza | Room 2.x nad SQLite |

---

## 0. Kako agenti koriste ovaj dokument

Ovo je **jedini izvor istine za strukturu podataka**. Pravila:

1. **Ne dodaj, ne preimenuj i ne uklanjaj kolone** bez odobrenja korisnika i podizanja verzije ovog dokumenta.
2. **Nazivi su obavezujući.** MySQL koristi `snake_case`, JSON i Kotlin koriste `camelCase`. Mapiranje je u sekciji 7 i mora se poštovati doslovno.
3. **Dve baze nisu ista stvar.** MySQL je autoritativni izvor; Room je lokalni izvor istine za UI i sadrži kolone kojih na serveru nema. Sekcija 6 tačno kaže koje.
4. Pre pisanja bilo kog upita pročitaj **sekciju 9 (pravila vlasništva)** — svaki upit nad `inventory_items` i `locations` mora filtrirati po vlasniku.
5. Pri svakoj promeni Room šeme **mora se podići `version`** u `@Database` i napisati migracija (sekcija 11).

---

## 1. Pregled entiteta

```
                 +------------------+
                 |      users       |
                 +------------------+
                 | id (PK)          |
                 | email (UNIQUE)   |
                 | role             |
                 | is_active        |
                 +------------------+
                    |            |
        1:N         |            |         1:N
     (vlasnistvo)   |            |     (vlasnistvo)
                    v            v
        +------------------+   +----------------------+
        |    locations     |   |   inventory_items    |
        +------------------+   +----------------------+
        | id (PK)          |   | id (PK)              |
        | user_id (FK)     |   | user_id (FK)         |
        | name             |<--| location_id (FK)     |
        +------------------+   | category_id (FK)     |
                               | deleted_at           |
                               +----------------------+
                                          ^
                                          | N:1
                               +----------------------+
                               |     categories       |   GLOBALNE
                               +----------------------+   (bez user_id)
                               | id (PK)              |
                               | name (UNIQUE)        |
                               +----------------------+
```

| Entitet | Vlasnik | Postoji u MySQL | Postoji u Room |
|---|---|:---:|:---:|
| `users` | sistem | da | da (samo prijavljeni korisnik) |
| `categories` | sistem (globalne) | da | da (keš) |
| `locations` | korisnik | da | da |
| `inventory_items` | korisnik | da | da |
| `exchange_rates` | — | ne | da (keš eksternog API-ja) |
| `sync_metadata` | — | ne | da (lokalno stanje sinhronizacije) |

## 2. Konvencije tipova podataka

Ovo je **najvažnija sekcija dokumenta**. Nedosledno rukovanje tipovima je glavni izvor bagova u ovakvom sistemu.

### 2.1 Identifikatori

| Aspekt | Odluka |
|---|---|
| Format | UUID verzija 4, mala slova, sa crticama — `550e8400-e29b-41d4-a716-446655440000` |
| MySQL | `CHAR(36)` — fiksna dužina, `utf8mb4_bin` collation radi tačnog poređenja |
| Room / Kotlin | `String` |
| Ko generiše | **Android klijent** pri kreiranju predmeta ili lokacije (`UUID.randomUUID().toString()`) |
| Izuzetak | `users.id` i `categories.id` generiše **server**, jer nastaju isključivo na serveru |

**Razlog:** korisnik može kreirati predmet bez interneta. ID mora postojati odmah i biti isti kad predmet konačno stigne na server — bez remapiranja.

### 2.2 Novac

**Sve novčane vrednosti su celi brojevi u minor jedinicama valute.**

| Aspekt | Odluka |
|---|---|
| MySQL | `BIGINT` (nullable gde je cena opciona) |
| Room / Kotlin | `Long?` |
| JSON | broj (`120000000`), nikada string, nikada decimala |
| Eksponent | 2 za sve podržane valute |
| Primer | 120.000,00 RSD se čuva kao `12000000` |
| Maksimum | 99.999.999.999 minor jedinica (VR-10) |

**Zabranjeno:** `Float`, `Double` i `REAL` za novac bilo gde u sistemu (NFR-12). Formatiranje za prikaz radi isključivo `MoneyFormatter` (definisan u `tech.md`).

### 2.3 Datumi i vremena

Postoje **dve različite vrste** i ne smeju se mešati.

**A) Kalendarski datum** — `purchase_date`, `warranty_expiration_date`

| Sloj | Tip | Primer |
|---|---|---|
| MySQL | `DATE` | `2024-03-15` |
| JSON | `String` | `"2024-03-15"` |
| Room | `TEXT` | `"2024-03-15"` |
| Kotlin domain | `LocalDate?` | |

Format je uvek `YYYY-MM-DD`. Nema vremena, nema vremenske zone. Ovaj format je **leksikografski sortabilan**, pa `ORDER BY purchase_date` i `WHERE purchase_date BETWEEN` rade ispravno i u SQLite-u.

**B) Trenutak u vremenu** — `created_at`, `updated_at`, `deleted_at`, `fetched_at`

| Sloj | Tip | Primer |
|---|---|---|
| MySQL | `DATETIME(3)` u **UTC** | `2026-08-22 14:07:31.123` |
| JSON | ISO-8601 sa `Z` | `"2026-08-22T14:07:31.123Z"` |
| Room | `INTEGER` (epoch milisekunde) | `1787407651123` |
| Kotlin domain | `Long` (epoch millis) | |

**Server nikada ne koristi lokalnu vremensku zonu.** MySQL konekcija se otvara sa `timezone: 'Z'`. Ovo je preduslov da last-write-wins poređenje (FR-095) uopšte bude tačno.

### 2.4 Enumeracije

Sve enumeracije se čuvaju kao **tekst velikim slovima**, nikada kao broj — brojevi postaju nečitljivi u bazi i lomljivi pri promeni redosleda.

| Enum | Dozvoljene vrednosti | MySQL tip | Room tip |
|---|---|---|---|
| `UserRole` | `USER`, `ADMIN` | `ENUM('USER','ADMIN')` | `TEXT` + TypeConverter |
| `Currency` | `RSD`, `EUR`, `USD`, `CHF`, `GBP`, `BAM` | `CHAR(3)` | `TEXT` |
| `SyncStatus` | `SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE` | — (samo lokalno) | `TEXT` + TypeConverter |
| `WarrantyStatus` | `AKTIVNA`, `USKORO_ISTICE`, `ISTEKLA`, `NEPOZNATO` | — (računa se) | — (računa se) |

`WarrantyStatus` se **nikada ne čuva u bazi** — izvodi se iz `warranty_expiration_date` po BR-010, jer zavisi od današnjeg datuma i korisnikovog praga.

### 2.5 Tekstualna polja

- Charset baze: `utf8mb4`, collation `utf8mb4_0900_ai_ci` (case- i accent-insensitive) — bitno da bi pretraga „samsung" pronašla „Samsung".
- Prazna opciona polja se čuvaju kao `NULL`, nikada kao `''` (BR-016).
- Svako tekstualno polje se `trim`-uje pre upisa (VR-20).

---

## 3. MySQL šema (backend)

Kompletan DDL. Ovaj kod ide u `backend/src/db/schema.sql`.

```sql
CREATE DATABASE IF NOT EXISTS home_inventory
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE home_inventory;

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE users (
  id            CHAR(36)      NOT NULL,
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(255)  NOT NULL,
  password_hash CHAR(60)      NOT NULL,          -- BCrypt je uvek 60 znakova
  role          ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  is_active     TINYINT(1)    NOT NULL DEFAULT 1,
  currency      CHAR(3)       NOT NULL DEFAULT 'RSD',  -- valuta prikaza
  created_at    DATETIME(3)   NOT NULL,
  updated_at    DATETIME(3)   NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- categories  (GLOBALNE - nemaju user_id, vidi BR-003)
-- ---------------------------------------------------------------
CREATE TABLE categories (
  id          CHAR(36)     NOT NULL,
  name        VARCHAR(60)  NOT NULL,
  description VARCHAR(255) NULL,
  icon_key    VARCHAR(40)  NULL,   -- kljuc drawable ikonice na Androidu
  sort_order  INT          NOT NULL DEFAULT 0,
  created_at  DATETIME(3)  NOT NULL,
  updated_at  DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_name (name)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- locations  (privatne po korisniku)
-- ---------------------------------------------------------------
CREATE TABLE locations (
  id          CHAR(36)     NOT NULL,
  user_id     CHAR(36)     NOT NULL,
  name        VARCHAR(60)  NOT NULL,
  description VARCHAR(255) NULL,
  created_at  DATETIME(3)  NOT NULL,
  updated_at  DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_locations_user_name (user_id, name),
  KEY idx_locations_user (user_id),
  CONSTRAINT fk_locations_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- inventory_items
-- ---------------------------------------------------------------
CREATE TABLE inventory_items (
  id                       CHAR(36)      NOT NULL,
  user_id                  CHAR(36)      NOT NULL,
  name                     VARCHAR(120)  NOT NULL,
  description              VARCHAR(1000) NULL,
  category_id              CHAR(36)      NOT NULL,
  location_id              CHAR(36)      NOT NULL,
  manufacturer             VARCHAR(100)  NULL,
  model                    VARCHAR(100)  NULL,
  serial_number            VARCHAR(100)  NULL,
  quantity                 INT           NOT NULL DEFAULT 1,
  purchase_price           BIGINT        NULL,   -- minor jedinice
  estimated_value          BIGINT        NULL,   -- minor jedinice
  currency                 CHAR(3)       NOT NULL DEFAULT 'RSD',
  purchase_date            DATE          NULL,
  warranty_expiration_date DATE          NULL,
  seller                   VARCHAR(100)  NULL,
  notes                    VARCHAR(1000) NULL,
  created_at               DATETIME(3)   NOT NULL,
  updated_at               DATETIME(3)   NOT NULL,
  deleted_at               DATETIME(3)   NULL,   -- soft delete, BR / FR-026
  PRIMARY KEY (id),
  KEY idx_items_user_updated  (user_id, updated_at),
  KEY idx_items_user_active   (user_id, deleted_at),
  KEY idx_items_category      (category_id),
  KEY idx_items_location      (location_id),
  KEY idx_items_user_warranty (user_id, warranty_expiration_date),
  CONSTRAINT fk_items_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_items_category
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
  CONSTRAINT fk_items_location
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE RESTRICT,
  CONSTRAINT chk_items_quantity CHECK (quantity >= 1 AND quantity <= 9999),
  CONSTRAINT chk_items_prices   CHECK (
        (purchase_price  IS NULL OR purchase_price  >= 0)
    AND (estimated_value IS NULL OR estimated_value >= 0)
  )
) ENGINE=InnoDB;
```

### Objašnjenje indeksa

| Indeks | Zašto postoji |
|---|---|
| `uq_users_email` | Sprovodi VR-03 na nivou baze, ne samo u aplikaciji |
| `uq_categories_name` | Sprovodi VR-18 |
| `uq_locations_user_name` | Sprovodi VR-19 — jedinstveno **po korisniku**, ne globalno |
| `idx_items_user_updated` | Nosi delta pull upit `WHERE user_id=? AND updated_at > ?` (FR-098) |
| `idx_items_user_active` | Nosi najčešći upit u sistemu: aktivni predmeti korisnika |
| `idx_items_category` / `idx_items_location` | Nose provere „koliko predmeta koristi ovaj entitet" pri brisanju (BR-014) |
| `idx_items_user_warranty` | Nosi upit za garancije koje uskoro ističu (FR-053) |

### `ON DELETE` politike i zašto su takve

| Relacija | Politika | Obrazloženje |
|---|---|---|
| `items.user_id → users.id` | `CASCADE` | Brisanje naloga uklanja njegov inventar |
| `locations.user_id → users.id` | `CASCADE` | Isto |
| `items.category_id → categories.id` | `RESTRICT` | Baza je poslednja odbrana za BR-014 |
| `items.location_id → locations.id` | `RESTRICT` | Isto |

**Napomena:** `RESTRICT` blokira i brisanje kategorije koju koristi samo *soft-obrisan* predmet. To je namerno — soft-obrisan red i dalje postoji i mora imati važeću referencu.

### Ograničenje koje strani ključ ne može da sprovede

**DB-RULE-01:** `inventory_items.location_id` mora pokazivati na lokaciju **istog korisnika** koji je vlasnik predmeta. MySQL ne može izraziti ovo strani ključem. Sprovodi se u aplikacionom sloju pri svakom `POST` i `PUT` predmeta:

```sql
SELECT id FROM locations WHERE id = ? AND user_id = ?
```

Ako ne vrati red → `404 NOT_FOUND` (ne `403`, po BR-002).

---

## 4. Seed podaci

### 4.1 Kategorije (globalne, FR-041)

Ubacuju se jednom, pri inicijalizaciji baze. ID-jeve generiše server.

| sort_order | name | icon_key |
|---|---|---|
| 1 | Elektronika | ic_category_electronics |
| 2 | Nameštaj | ic_category_furniture |
| 3 | Bela tehnika | ic_category_appliances |
| 4 | Kuhinja | ic_category_kitchen |
| 5 | Odeća | ic_category_clothing |
| 6 | Alat | ic_category_tools |
| 7 | Sportska oprema | ic_category_sports |
| 8 | Vozila | ic_category_vehicles |
| 9 | Dekoracija | ic_category_decor |
| 10 | Dokumenta | ic_category_documents |
| 11 | Ostalo | ic_category_other |

### 4.2 Lokacije (po korisniku, BR-015)

Kreiraju se automatski u istoj transakciji kao i sam korisnik, pri registraciji:

Dnevna soba, Spavaća soba, Kuhinja, Kupatilo, Garaža, Podrum, Tavan, Radna soba, Hodnik

### 4.3 Demo inventar (FR — „rad sa većom količinom podataka")

Skripta `backend/src/db/seed.js`, pokreće se sa `npm run seed`. Kreira demo nalog i puni ga.

| Parametar | Vrednost |
|---|---|
| Demo nalog | `demo@homeinventory.rs` / `Demo1234` |
| Broj predmeta | ~60 |
| Raspodela po kategorijama | sve 11 kategorija zastupljene, Elektronika najbrojnija |
| Raspodela po lokacijama | svih 9 lokacija zastupljeno |
| Valute | ~70% RSD, ~25% EUR, ~5% USD — da konverzija ima šta da pokaže |
| Raspon cena | od 500 RSD do 250.000 RSD |
| Garancije | ~25% aktivna, ~15% ističe u narednih 30 dana, ~20% istekla, ~40% bez datuma |
| Datumi kupovine | raspoređeni kroz poslednje 4 godine — da filter po godini ima smisla |
| Serijski brojevi | popunjeni kod elektronike i bele tehnike, prazni drugde |

**Seed mora biti idempotentan** — ponovno pokretanje briše demo nalog i kreira ga iznova, ne duplira podatke. Seed **ne dira** stvarne korisničke naloge.

---

## 5. Room šema (Android)

Paket: `rs.homeinventory.app.data.local`

### 5.1 Entity klase

```kotlin
// ---------- UserEntity ----------
// Kesira SAMO trenutno prijavljenog korisnika. Uvek najvise jedan red.
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole,          // TypeConverter
    val isActive: Boolean,
    val currency: String,        // valuta prikaza
    val createdAt: Long,
    val updatedAt: Long
)

// ---------- CategoryEntity ----------
// Kes globalnih kategorija sa servera. Klijent ih nikada ne menja.
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val iconKey: String?,
    val sortOrder: Int,
    val updatedAt: Long
)

// ---------- LocationEntity ----------
@Entity(
    tableName = "locations",
    indices = [Index(value = ["userId"]), Index(value = ["userId", "name"], unique = true)]
)
data class LocationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

// ---------- InventoryItemEntity ----------
@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["userId", "deletedAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["locationId"]),
        Index(value = ["userId", "warrantyExpirationDate"]),
        Index(value = ["syncStatus"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"], childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"], childColumns = ["locationId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,              // UUID v4, generise klijent
    val userId: String,
    val name: String,
    val description: String?,
    val categoryId: String,
    val locationId: String,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val quantity: Int,
    val purchasePrice: Long?,                // minor jedinice
    val estimatedValue: Long?,               // minor jedinice
    val currency: String,
    val purchaseDate: String?,               // "YYYY-MM-DD"
    val warrantyExpirationDate: String?,     // "YYYY-MM-DD"
    val seller: String?,
    val notes: String?,
    val createdAt: Long,                     // epoch millis
    val updatedAt: Long,                     // epoch millis
    val deletedAt: Long?,                    // soft delete

    // --- SAMO LOKALNO, ne postoji na serveru ---
    val imagePath: String?,                  // naziv fajla u internal storage
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

// ---------- ExchangeRateEntity ----------
// Kes eksternog Currency API-ja. Ne postoji na nasem backendu.
@Entity(tableName = "exchange_rates", primaryKeys = ["baseCode", "targetCode"])
data class ExchangeRateEntity(
    val baseCode: String,     // uvek "EUR" - API se poziva sa EUR bazom
    val targetCode: String,   // RSD, USD, CHF, GBP, BAM, EUR
    val rate: Double,         // kurs, NIJE novac - Double je ovde ispravan
    val fetchedAt: Long       // epoch millis, za TTL od 24h
)

// ---------- SyncMetadataEntity ----------
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String,   // npr. "items_last_sync_at"
    val value: String
)
```

### 5.2 TypeConverters

```kotlin
class Converters {
    @TypeConverter fun roleToString(v: UserRole): String = v.name
    @TypeConverter fun stringToRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun syncToString(v: SyncStatus): String = v.name
    @TypeConverter fun stringToSync(v: String): SyncStatus = SyncStatus.valueOf(v)
}
```

Namerno **nema** konvertera za datume: kalendarski datumi su već `String`, a trenuci su već `Long`. Manje konvertera znači manje mesta gde se greši.

### 5.3 Database klasa

```kotlin
@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        LocationEntity::class,
        InventoryItemEntity::class,
        ExchangeRateEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HomeInventoryDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
```

`exportSchema = true` uz `room.schemaLocation` — JSON šema se commit-uje u repo, što je preduslov za pisanje migracija.

### 5.4 Projekcije za liste

Lista inventara mora prikazati naziv kategorije i lokacije, a pretraga mora pretraživati po njima (FR-031). Zato postoji namenska projekcija umesto `@Relation`:

```kotlin
data class ItemListRow(
    val id: String,
    val name: String,
    val manufacturer: String?,
    val model: String?,
    val quantity: Int,
    val purchasePrice: Long?,
    val estimatedValue: Long?,
    val currency: String,
    val purchaseDate: String?,
    val warrantyExpirationDate: String?,
    val imagePath: String?,
    val createdAt: Long,
    val syncStatus: SyncStatus,
    val categoryId: String,
    val categoryName: String,
    val categoryIconKey: String?,
    val locationId: String,
    val locationName: String
)

data class CategoryAggregate(
    val categoryId: String,
    val categoryName: String,
    val currency: String,
    val itemCount: Int,        // zbir quantity
    val totalMinor: Long       // zbir efektivne vrednosti u toj valuti
)
```

**Zašto `CategoryAggregate` grupiše i po valuti:** SQL ne može sabrati 120.000 RSD i 900 EUR. DAO vraća zbirove **po valuti**, a konverziju u valutu prikaza radi Kotlin sloj po BR-009. Ovo je namerno i ne sme se „pojednostaviti".

---

## 6. Šta postoji gde

| Polje | MySQL | Room | Napomena |
|---|:---:|:---:|---|
| `users.password_hash` | da | **ne** | Hash nikada ne napušta server |
| `inventory_items.*` (poslovna polja) | da | da | Sinhronizuju se u oba smera |
| `inventory_items.image_path` | **ne** | da | FR-084: fotografija je lokalna |
| `inventory_items.sync_status` | **ne** | da | Čisto lokalno stanje |
| `locations.sync_status` | **ne** | da | Isto |
| `exchange_rates` | **ne** | da | Keš eksternog API-ja |
| `sync_metadata` | **ne** | da | Lokalno stanje sinhronizacije |
| `categories.*` | da | da | Server piše, klijent samo čita |

**DB-RULE-02 — Sinhronizacija ne sme obrisati lokalna polja.** Kada pull sa servera prepiše postojeći red, `imagePath` **se mora sačuvati** (FR-085). Merge se radi eksplicitno u `SyncManager`-u, nikad prostim `INSERT OR REPLACE` nad celim redom.

---

## 7. Mapiranje polja kroz slojeve

Autoritativna tabela. Svako odstupanje je bag.

| MySQL kolona | JSON polje | Room polje | Domain polje | Tip u JSON-u |
|---|---|---|---|---|
| `id` | `id` | `id` | `id` | string (UUID) |
| `user_id` | `userId` | `userId` | `userId` | string |
| `name` | `name` | `name` | `name` | string |
| `description` | `description` | `description` | `description` | string / null |
| `category_id` | `categoryId` | `categoryId` | `category.id` | string |
| `location_id` | `locationId` | `locationId` | `location.id` | string |
| `manufacturer` | `manufacturer` | `manufacturer` | `manufacturer` | string / null |
| `model` | `model` | `model` | `model` | string / null |
| `serial_number` | `serialNumber` | `serialNumber` | `serialNumber` | string / null |
| `quantity` | `quantity` | `quantity` | `quantity` | number |
| `purchase_price` | `purchasePrice` | `purchasePrice` | `purchasePrice` | number / null (minor) |
| `estimated_value` | `estimatedValue` | `estimatedValue` | `estimatedValue` | number / null (minor) |
| `currency` | `currency` | `currency` | `currency` | string (3 znaka) |
| `purchase_date` | `purchaseDate` | `purchaseDate` | `purchaseDate` | string `YYYY-MM-DD` / null |
| `warranty_expiration_date` | `warrantyExpirationDate` | `warrantyExpirationDate` | `warrantyExpirationDate` | string `YYYY-MM-DD` / null |
| `seller` | `seller` | `seller` | `seller` | string / null |
| `notes` | `notes` | `notes` | `notes` | string / null |
| `created_at` | `createdAt` | `createdAt` | `createdAt` | ISO-8601 UTC string |
| `updated_at` | `updatedAt` | `updatedAt` | `updatedAt` | ISO-8601 UTC string |
| `deleted_at` | `deletedAt` | `deletedAt` | `deletedAt` | ISO-8601 UTC string / null |
| — | — | `imagePath` | `imagePath` | ne prenosi se |
| — | — | `syncStatus` | `syncStatus` | ne prenosi se |

**Konverzija tipa `createdAt`:** MySQL `DATETIME(3)` → JSON ISO string → Room `Long`. Konverzija postoji na tačno dva mesta: `DateMapper.kt` na Androidu i `serializer.js` na backendu. Nigde drugde.

---

## 8. Ključni upiti

### 8.1 Room DAO — potpisi

```kotlin
@Dao
interface ItemDao {

    // ---- Citanje ----
    @Query("""
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
    """)
    fun observeAll(userId: String): Flow<List<ItemListRow>>

    // ---- Pretraga po SEST polja (FR-031) ----
    @Query("""
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
               i.name          LIKE '%' || :query || '%' COLLATE NOCASE OR
               i.manufacturer  LIKE '%' || :query || '%' COLLATE NOCASE OR
               i.model         LIKE '%' || :query || '%' COLLATE NOCASE OR
               i.serialNumber  LIKE '%' || :query || '%' COLLATE NOCASE OR
               c.name          LIKE '%' || :query || '%' COLLATE NOCASE OR
               l.name          LIKE '%' || :query || '%' COLLATE NOCASE)
    """)
    fun search(userId: String, query: String): Flow<List<ItemListRow>>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<InventoryItemEntity?>

    // ---- Garancije (FR-053) ----
    @Query("""
        SELECT * FROM inventory_items
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND warrantyExpirationDate IS NOT NULL
          AND warrantyExpirationDate >= :today
          AND warrantyExpirationDate <= :thresholdDate
        ORDER BY warrantyExpirationDate ASC
    """)
    fun observeExpiringWarranties(
        userId: String, today: String, thresholdDate: String
    ): Flow<List<InventoryItemEntity>>

    // ---- Agregacija po kategoriji I valuti (BR-009) ----
    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, i.currency AS currency,
               SUM(i.quantity) AS itemCount,
               SUM(COALESCE(i.estimatedValue, i.purchasePrice, 0) * i.quantity) AS totalMinor
        FROM inventory_items i
        JOIN categories c ON c.id = i.categoryId
        WHERE i.userId = :userId AND i.deletedAt IS NULL
        GROUP BY c.id, c.name, i.currency
    """)
    fun observeCategoryAggregates(userId: String): Flow<List<CategoryAggregate>>

    // ---- Provere pre brisanja (BR-014) ----
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

    // Fizicko uklanjanje tek POSLE potvrde servera
    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM inventory_items")
    suspend fun clear()
}
```

**Napomena o `COLLATE NOCASE`:** radi pouzdano za ASCII. Za srpske dijakritike (Č, Ć, Š, Ž, Đ) SQLite `NOCASE` **ne radi**. Rešenje koje se primenjuje: pri upisu se popunjava pomoćna kolona sa normalizovanim tekstom, ILI se pretraga radi nad `LOWER()` vrednostima uz Kotlin normalizaciju upita. **Odluka: normalizacija u Kotlin sloju pre slanja upita** — `query.lowercase()` plus poređenje nad `LOWER(i.name)`. Detalj implementacije je u `tech.md`.

### 8.2 Backend — obavezni obrazac upita

**Svaki** upit nad `inventory_items` i `locations` mora sadržati `user_id` iz JWT tokena:

```sql
-- ISPRAVNO
SELECT * FROM inventory_items WHERE id = ? AND user_id = ? AND deleted_at IS NULL;

-- POGRESNO - ranjivost, krsi BR-002
SELECT * FROM inventory_items WHERE id = ?;
```

Delta pull (FR-098) — vraća i tombstone redove da bi klijent znao šta je obrisano:

```sql
SELECT * FROM inventory_items
WHERE user_id = ? AND updated_at > ?
ORDER BY updated_at ASC
LIMIT 500;
```

---

## 9. Pravila vlasništva

| ID | Pravilo |
|---|---|
| **OWN-01** | `user_id` se **uvek** uzima iz verifikovanog JWT tokena, nikada iz tela zahteva ili URL-a |
| **OWN-02** | Ako klijent pošalje `userId` u telu zahteva, server ga **ignoriše** |
| **OWN-03** | Pristup tuđem resursu vraća `404`, ne `403` — da se ne otkriva postojanje resursa |
| **OWN-04** | `location_id` mora pripadati istom korisniku (DB-RULE-01) |
| **OWN-05** | Kategorije su globalne — nema provere vlasništva, ali izmena zahteva rolu ADMIN |
| **OWN-06** | Admin endpointi ne izlažu sadržaj tuđeg inventara, samo agregatne brojeve |
| **OWN-07** | Room baza sadrži podatke samo jednog korisnika; odjava je briše u celosti (BR-005) |

---

## 10. Stanja sinhronizacije

| `syncStatus` | Značenje | Prikaz u UI-ju | Šta radi sync |
|---|---|---|---|
| `SYNCED` | Poklapa se sa serverom | normalno | ništa |
| `PENDING_CREATE` | Kreiran lokalno, server ga ne zna | ikonica oblaka sa strelicom | `POST /api/items` |
| `PENDING_UPDATE` | Izmenjen lokalno | ikonica oblaka sa strelicom | `PUT /api/items/:id` |
| `PENDING_DELETE` | Obrisan lokalno | ne prikazuje se u listi | `DELETE /api/items/:id`, pa `hardDelete` lokalno |

### Prelazi stanja

```
[ne postoji] --kreiranje--> PENDING_CREATE --sync ok--> SYNCED
                                  |
                                  +--izmena pre sync-a--> PENDING_CREATE  (ostaje isto)
                                  +--brisanje pre sync-a--> hardDelete odmah, bez poziva servera

SYNCED --izmena--> PENDING_UPDATE --sync ok--> SYNCED
SYNCED --brisanje--> PENDING_DELETE --sync ok--> hardDelete lokalno
```

**DB-RULE-03 — Predmet kreiran offline pa obrisan offline se briše fizički, bez poziva servera.** Server za njega nikada nije ni saznao.

**DB-RULE-04 — Rešavanje konflikta (FR-095).** Pri push-u klijent šalje svoj `updatedAt`. Server poredi:
- `clientUpdatedAt >= serverUpdatedAt` → prihvata izmenu, upisuje `updated_at = NOW(3)`, vraća `200` sa novim redom
- `clientUpdatedAt < serverUpdatedAt` → odbija sa `409 SYNC_CONFLICT` i vraća serversku verziju; klijent njome prepisuje lokalni red (uz očuvanje `imagePath`, DB-RULE-02)

---

## 11. Migracije

### MySQL

Nema formalnog migracionog alata. Šema se održava kroz `backend/src/db/schema.sql`, koji je pisan idempotentno (`CREATE TABLE IF NOT EXISTS`). Skripte:

| Komanda | Efekat |
|---|---|
| `npm run db:create` | Kreira bazu i tabele iz `schema.sql` |
| `npm run db:reset` | `DROP DATABASE` pa ponovo kreira — **samo u razvoju** |
| `npm run seed` | Puni kategorije i demo nalog |

Svaka izmena šeme posle Faze 2 se dodaje kao numerisani fajl `backend/src/db/migrations/00X_opis.sql` i istovremeno se ažurira `schema.sql`.

### Room

| Pravilo | Detalj |
|---|---|
| Verzija 1 | Početna šema iz sekcije 5 |
| Svaka izmena entiteta | `version` se povećava i piše se `Migration(n, n+1)` |
| `fallbackToDestructiveMigration()` | **Zabranjeno** u kodu koji ide u isporuku |
| `exportSchema` | `true`; JSON šeme se commit-uju u `android/app/schemas/` |
| Testiranje | `MigrationTestHelper` test za svaku migraciju |

**Izuzetak:** dok traje Faza 3, dok se šema još stabilizuje, dozvoljeno je `fallbackToDestructiveMigration()`. **Mora se ukloniti pre kraja Faze 4** i to je stavka acceptance checkliste.

---

## 12. Procena veličine podataka

| Stavka | Veličina | Napomena |
|---|---|---|
| Jedan red `inventory_items` | ~400–600 B | Bez fotografije |
| 500 predmeta | ~300 KB | Zanemarljivo za SQLite |
| Jedna fotografija posle kompresije | ~150–400 KB | 1080 px, JPEG 80 (FR-082) |
| 60 fotografija | ~15–25 MB | U internal storage, **ne u bazi** |
| `exchange_rates` | < 1 KB | Šest valuta |

Ovo je i razlog za odluku iz FR-083: da su fotografije u bazi kao BLOB, baza bi sa 60 predmeta prešla 20 MB i svaki upit bi usporio.

---

## 13. Kontrolna lista pre kraja Faze 2

- [ ] `schema.sql` se izvršava bez greške na čistoj MySQL 8.0 instanci
- [ ] Svi indeksi iz sekcije 3 postoje (`SHOW INDEX FROM inventory_items`)
- [ ] `uq_users_email` odbija duplikat email adrese
- [ ] `uq_locations_user_name` dozvoljava istu „Kuhinja" kod dva različita korisnika
- [ ] `RESTRICT` blokira brisanje kategorije u upotrebi
- [ ] `CHECK` ograničenje odbija `quantity = 0` i negativnu cenu
- [ ] Konekcija je u UTC (`SELECT @@session.time_zone` vraća `+00:00`)
- [ ] `npm run seed` je idempotentan — dvostruko pokretanje daje isti rezultat
- [ ] Nijedan endpoint ne izvršava upit nad `inventory_items` bez `user_id` uslova

---

**Kraj dokumenta.** Izmene zahtevaju odobrenje korisnika i podizanje verzije u zaglavlju.
