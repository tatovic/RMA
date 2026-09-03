-- Home Inventory — MySQL šema
-- Izvor istine: db.md sekcija 3. Ne menjati bez ažuriranja tog dokumenta.
-- Idempotentno: CREATE TABLE IF NOT EXISTS, sme se pokretati više puta.
--
-- Fajl sadrži ISKLJUČIVO tabele i izvršava se nad već izabranom bazom. Kreiranje same baze
-- (i njeno ime iz env.DB_NAME) je posao createDatabase.js — ranije je ime bilo zakucano ovde,
-- pa je DB_NAME iz .env bio ignorisan i test/produkciona baza nisu mogle da se razdvoje.

-- ---------------------------------------------------------------
-- users
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id            CHAR(36)      NOT NULL,
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(255)  NOT NULL,
  password_hash CHAR(60)      NOT NULL,          -- BCrypt je uvek 60 znakova
  role          ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  is_active     TINYINT(1)    NOT NULL DEFAULT 1,
  currency      CHAR(3)       NOT NULL DEFAULT 'RSD',  -- valuta prikaza
  -- Verzija tokena: povećava se pri promeni lozinke, čime svaki ranije izdat JWT prestaje da važi.
  -- Nosi se kao `tv` claim i poredi se u middleware/authenticate.js (tiket 28).
  token_version INT           NOT NULL DEFAULT 0,
  created_at    DATETIME(3)   NOT NULL,
  updated_at    DATETIME(3)   NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- categories  (GLOBALNE - nemaju user_id, vidi BR-003)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
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
CREATE TABLE IF NOT EXISTS locations (
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
CREATE TABLE IF NOT EXISTS inventory_items (
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
  -- CASCADE, ne RESTRICT (tiket 28, blokirajući nalaz 02). Izgleda opasnije nego što jeste:
  -- BR-014 guard u locations.service.js/categories.service.js odbija brisanje dok postoji ijedan
  -- ŽIV predmet (deleted_at IS NULL), pa kaskada u praksi može da dohvati isključivo tombstone
  -- redove — soft-obrisane predmete koje korisnik više ne vidi. Sa RESTRICT ti tombstone redovi
  -- su držali strani ključ u životu i svako brisanje lokacije/kategorije posle prvog brisanja
  -- predmeta je zauvek vraćalo 500 (ER_ROW_IS_REFERENCED_2).
  -- Kod kategorija kaskada prelazi granicu korisnika (kategorije su globalne, BR-003); to je
  -- prihvatljivo isključivo zato što je i BR-014 guard globalan — broji predmete SVIH korisnika.
  CONSTRAINT fk_items_category
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
  CONSTRAINT fk_items_location
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE,
  CONSTRAINT chk_items_quantity CHECK (quantity >= 1 AND quantity <= 9999),
  CONSTRAINT chk_items_prices   CHECK (
        (purchase_price  IS NULL OR purchase_price  >= 0)
    AND (estimated_value IS NULL OR estimated_value >= 0)
  )
) ENGINE=InnoDB;
