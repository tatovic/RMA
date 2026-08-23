// Demo inventar — db.md sekcija 4.3, tiket 07.
// Idempotentno: obriše postojeći demo nalog (CASCADE briše i lokacije i predmete) pa ga kreira iznova.
// Ne dira stvarne korisničke naloge — jedini filter je e-mail demo naloga.
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');

const pool = require('../config/db');
const env = require('../config/env');
const DEFAULT_LOCATIONS = require('./locations');

const DEMO_EMAIL = 'demo@homeinventory.rs';
const DEMO_PASSWORD = 'Demo1234';
const DEMO_NAME = 'Demo Korisnik';

// Prodavci koji se pojavljuju kao `seller` — samo za realističniji demo prikaz.
const SELLERS = [
  'Gigatron',
  'Tehnomanija',
  'Win Win Computers',
  'Emmezeta',
  'JYSK',
  'Merkator',
  'Metro',
  'Lilly Drogerie',
  'Sport Vision',
  'Intersport',
  'Polovni automobili — oglas',
  'Direktna kupovina od vlasnika',
];

// categoryName mora tačno odgovarati nazivima iz ./categories.js.
// hasSerial se ne postavlja ovde — automatski važi za Elektroniku i Belu tehniku (VR iz tiketa).
const ITEMS = [
  // ---- Elektronika (10) ----
  { categoryName: 'Elektronika', name: 'Samsung Galaxy S23 telefon', manufacturer: 'Samsung', model: 'Galaxy S23' },
  { categoryName: 'Elektronika', name: 'Dell XPS 13 laptop', manufacturer: 'Dell', model: 'XPS 13 9315' },
  { categoryName: 'Elektronika', name: 'Sony WH-1000XM5 slušalice', manufacturer: 'Sony', model: 'WH-1000XM5' },
  { categoryName: 'Elektronika', name: 'LG OLED televizor 55"', manufacturer: 'LG', model: 'OLED55C3' },
  { categoryName: 'Elektronika', name: 'Logitech MX Master miš', manufacturer: 'Logitech', model: 'MX Master 3S' },
  { categoryName: 'Elektronika', name: 'Apple iPad Air tablet', manufacturer: 'Apple', model: 'iPad Air 5. gen' },
  { categoryName: 'Elektronika', name: 'Canon EOS fotoaparat', manufacturer: 'Canon', model: 'EOS 2000D' },
  { categoryName: 'Elektronika', name: 'Xiaomi Mi Band narukvica', manufacturer: 'Xiaomi', model: 'Mi Band 7' },
  { categoryName: 'Elektronika', name: 'JBL Charge zvučnik', manufacturer: 'JBL', model: 'Charge 5' },
  { categoryName: 'Elektronika', name: 'Asus ROG kućni računar', manufacturer: 'Asus', model: 'ROG Strix G10' },

  // ---- Nameštaj (6) ----
  { categoryName: 'Nameštaj', name: 'IKEA Malm komoda', manufacturer: 'IKEA', model: 'MALM' },
  { categoryName: 'Nameštaj', name: 'Trosed na razvlačenje' },
  { categoryName: 'Nameštaj', name: 'Radni sto od hrasta' },
  { categoryName: 'Nameštaj', name: 'Ormar za odeću, trokrilni' },
  { categoryName: 'Nameštaj', name: 'Kuhinjske stolice (set od 4)', quantity: 4 },
  { categoryName: 'Nameštaj', name: 'Polica za knjige', manufacturer: 'IKEA', model: 'BILLY' },

  // ---- Bela tehnika (6) ----
  { categoryName: 'Bela tehnika', name: 'Mašina za veš', manufacturer: 'Bosch', model: 'WAN28281BY' },
  { categoryName: 'Bela tehnika', name: 'Frižider', manufacturer: 'Samsung', model: 'RB34T632ESA' },
  { categoryName: 'Bela tehnika', name: 'Ugradna rerna', manufacturer: 'Gorenje', model: 'BO635E12X' },
  { categoryName: 'Bela tehnika', name: 'Mašina za sudove', manufacturer: 'Beko', model: 'BDFN15421W' },
  { categoryName: 'Bela tehnika', name: 'Mikrotalasna rerna', manufacturer: 'Candy', model: 'CMG20SDW' },
  { categoryName: 'Bela tehnika', name: 'Usisivač', manufacturer: 'Electrolux', model: 'EER71SM' },

  // ---- Kuhinja (6) ----
  { categoryName: 'Kuhinja', name: 'Set lonaca', manufacturer: 'Tefal', model: 'Duetto+' },
  { categoryName: 'Kuhinja', name: 'Blender', manufacturer: 'Philips', model: 'HR2224' },
  { categoryName: 'Kuhinja', name: 'Espresso aparat', manufacturer: 'DeLonghi', model: 'EC685' },
  { categoryName: 'Kuhinja', name: 'Toster', manufacturer: 'Bosch', model: 'TAT3P421' },
  { categoryName: 'Kuhinja', name: 'Set noževa', manufacturer: 'Fiskars', model: 'Functional Form' },
  { categoryName: 'Kuhinja', name: 'Friteza na vrući vazduh', manufacturer: 'Tefal', model: 'Easy Fry' },

  // ---- Odeća (5) ----
  { categoryName: 'Odeća', name: 'Zimska jakna', manufacturer: 'The North Face' },
  { categoryName: 'Odeća', name: 'Kožne cipele', manufacturer: 'Ecco' },
  { categoryName: 'Odeća', name: 'Farmerke', manufacturer: "Levi's", model: '501' },
  { categoryName: 'Odeća', name: 'Sportske patike', manufacturer: 'Nike', model: 'Air Max 90' },
  { categoryName: 'Odeća', name: 'Odelo za svečane prilike' },

  // ---- Alat (5) ----
  { categoryName: 'Alat', name: 'Bušilica', manufacturer: 'Bosch Professional', model: 'GSB 18V-55' },
  { categoryName: 'Alat', name: 'Set ključeva', manufacturer: 'Makita' },
  { categoryName: 'Alat', name: 'Ubodna testera', manufacturer: 'Bosch', model: 'GST 18V-Li' },
  { categoryName: 'Alat', name: 'Ekscentar brusilica', manufacturer: 'Makita', model: 'BO5041' },
  { categoryName: 'Alat', name: 'Kompresor za vazduh', manufacturer: 'Einhell', model: 'TC-AC 190' },

  // ---- Sportska oprema (5) ----
  { categoryName: 'Sportska oprema', name: 'Bicikl', manufacturer: 'Trek', model: 'FX2 Disc' },
  { categoryName: 'Sportska oprema', name: 'Set tegova 20kg' },
  { categoryName: 'Sportska oprema', name: 'Traka za trčanje', manufacturer: 'Hop-Sport', model: 'HS-2010' },
  { categoryName: 'Sportska oprema', name: 'Reket za tenis', manufacturer: 'Wilson', model: 'Pro Staff 97' },
  { categoryName: 'Sportska oprema', name: 'Šator za kampovanje', manufacturer: 'Ferrino', model: 'Sling 3' },

  // ---- Vozila (3) ----
  { categoryName: 'Vozila', name: 'Automobil', manufacturer: 'Škoda', model: 'Octavia III' },
  { categoryName: 'Vozila', name: 'Skuter', manufacturer: 'Yamaha', model: 'NMAX 125' },
  { categoryName: 'Vozila', name: 'Prikolica za bicikle' },

  // ---- Dekoracija (5) ----
  { categoryName: 'Dekoracija', name: 'Slika na platnu' },
  { categoryName: 'Dekoracija', name: 'Tepih, skandinavski dizajn' },
  { categoryName: 'Dekoracija', name: 'Stona lampa', manufacturer: 'IKEA', model: 'RANARP' },
  { categoryName: 'Dekoracija', name: 'Ogledalo u ramu' },
  { categoryName: 'Dekoracija', name: 'Vaza, keramika' },

  // ---- Dokumenta (4) — nisu kupljeni predmeti, cena se ne vodi ----
  { categoryName: 'Dokumenta', name: 'Ugovor o kupovini stana', noPrice: true },
  { categoryName: 'Dokumenta', name: 'Polisa osiguranja stana', noPrice: true },
  { categoryName: 'Dokumenta', name: 'Garantni list za kuću', noPrice: true },
  { categoryName: 'Dokumenta', name: 'Tehnički pasoš vozila', noPrice: true },

  // ---- Ostalo (5) ----
  { categoryName: 'Ostalo', name: 'Kofer za putovanja', manufacturer: 'Samsonite', model: "S'Cure" },
  { categoryName: 'Ostalo', name: 'Ruksak planinarski', manufacturer: 'Deuter', model: 'Aircontact 55' },
  { categoryName: 'Ostalo', name: 'Set alata za kampovanje' },
  { categoryName: 'Ostalo', name: 'Kišobran' },
  { categoryName: 'Ostalo', name: 'Termos boca', manufacturer: 'Stanley', model: 'Classic' },
];

const ITEM_COUNT = ITEMS.length; // mora biti 60 (tiket 07)

// Deterministički pseudo-random u [0, 1) — isti seed uvek daje isti raspored.
const rand01 = (n) => {
  const x = Math.sin(n * 12.9898) * 43758.5453;
  return x - Math.floor(x);
};

const addDays = (date, days) => {
  const copy = new Date(date.getTime());
  copy.setUTCDate(copy.getUTCDate() + days);
  return copy;
};

const toDateOnly = (date) => date.toISOString().slice(0, 10);

// Množioci uzajamno prosti sa ITEM_COUNT (2^2 * 3 * 5 za 60) — mešaju raspodelu po indeksima
// tako da valuta/garancija/datum ne prate redosled kategorija u nizu ITEMS.
const permute = (i, multiplier) => (i * multiplier) % ITEM_COUNT;

// ~70% RSD, ~25% EUR, ~5% USD (db.md sekcija 4.3).
const buildCurrencyPlan = () => {
  const rsdCount = Math.round(ITEM_COUNT * 0.7);
  const eurCount = Math.round(ITEM_COUNT * 0.25);
  const usdCount = ITEM_COUNT - rsdCount - eurCount;
  return [...Array(rsdCount).fill('RSD'), ...Array(eurCount).fill('EUR'), ...Array(usdCount).fill('USD')];
};

// ~25% aktivna, ~15% ističe u 30 dana, ~20% istekla, ~40% bez datuma (db.md sekcija 4.3).
const buildWarrantyPlan = () => {
  const activeCount = Math.round(ITEM_COUNT * 0.25);
  const expiringCount = Math.round(ITEM_COUNT * 0.15);
  const expiredCount = Math.round(ITEM_COUNT * 0.2);
  const noneCount = ITEM_COUNT - activeCount - expiringCount - expiredCount;
  return [
    ...Array(activeCount).fill('ACTIVE'),
    ...Array(expiringCount).fill('EXPIRING'),
    ...Array(expiredCount).fill('EXPIRED'),
    ...Array(noneCount).fill('NONE'),
  ];
};

const PRICE_RANGES = {
  RSD: { min: 500, max: 250000, round: 100 },
  EUR: { min: 5, max: 2500, round: 5 },
  USD: { min: 5, max: 2500, round: 5 },
};

const buildPrice = (currency, seed) => {
  const { min, max, round } = PRICE_RANGES[currency];
  const raw = min + rand01(seed) * (max - min);
  return Math.round(raw / round) * round;
};

const buildWarrantyDate = (today, category, seed) => {
  switch (category) {
    case 'ACTIVE':
      return toDateOnly(addDays(today, 40 + Math.floor(rand01(seed) * 700)));
    case 'EXPIRING':
      return toDateOnly(addDays(today, 1 + Math.floor(rand01(seed) * 29)));
    case 'EXPIRED':
      return toDateOnly(addDays(today, -(10 + Math.floor(rand01(seed) * 700))));
    default:
      return null;
  }
};

// Gradi 60 kompletnih redova spremnih za INSERT — čisto sinhrona funkcija, lako testabilna.
const buildDemoItems = ({ today, categoryIdByName, locationIds }) => {
  const currencyPlan = buildCurrencyPlan();
  const warrantyPlan = buildWarrantyPlan();

  // Datumi kupovine ravnomerno raspoređeni kroz poslednje 4 godine (0 do ~1460 dana unazad).
  const dateOffsets = Array.from({ length: ITEM_COUNT }, (_, i) => Math.round((i / (ITEM_COUNT - 1)) * 4 * 365));

  return ITEMS.map((def, i) => {
    const categoryId = categoryIdByName[def.categoryName];
    if (!categoryId) {
      throw new Error(`Nepoznata kategorija u seed listi: "${def.categoryName}". Da li je npm run db:create pokrenut?`);
    }

    const currency = currencyPlan[permute(i, 13)];
    const warrantyCategory = warrantyPlan[permute(i, 7)];
    const purchaseDate = toDateOnly(addDays(today, -dateOffsets[permute(i, 29)]));
    const warrantyExpirationDate = buildWarrantyDate(today, warrantyCategory, i + 4000);

    const hasSerial = def.categoryName === 'Elektronika' || def.categoryName === 'Bela tehnika';

    let purchasePrice = null;
    let estimatedValue = null;
    if (!def.noPrice) {
      purchasePrice = buildPrice(currency, i + 1) * 100; // minor jedinice
      const depreciation = 0.5 + rand01(i + 2000) * 0.45;
      estimatedValue = Math.round((purchasePrice * depreciation) / 100) * 100;
    }

    return {
      id: uuidv4(),
      name: def.name,
      description: null,
      categoryId,
      locationId: locationIds[i % locationIds.length],
      manufacturer: def.manufacturer ?? null,
      model: def.model ?? null,
      serialNumber: hasSerial ? `SN-${uuidv4().slice(0, 8).toUpperCase()}` : null,
      quantity: def.quantity ?? 1,
      purchasePrice,
      estimatedValue,
      currency,
      purchaseDate,
      warrantyExpirationDate,
      seller: SELLERS[i % SELLERS.length],
      notes: null,
    };
  });
};

const deleteExistingDemoUser = async (connection) => {
  // FK-ovi na users.id su ON DELETE CASCADE (db.md sekcija 3) — briše i lokacije i predmete demo naloga.
  const [rows] = await connection.query('SELECT id FROM users WHERE email = ? LIMIT 1', [DEMO_EMAIL]);
  if (rows.length > 0) {
    await connection.query('DELETE FROM users WHERE id = ?', [rows[0].id]);
  }
};

const insertDemoUser = async (connection) => {
  const passwordHash = await bcrypt.hash(DEMO_PASSWORD, env.BCRYPT_ROUNDS);
  const userId = uuidv4();

  await connection.query(
    `INSERT INTO users (id, name, email, password_hash, role, is_active, currency, created_at, updated_at)
     VALUES (?, ?, ?, ?, 'USER', 1, 'RSD', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
    [userId, DEMO_NAME, DEMO_EMAIL, passwordHash]
  );

  return userId;
};

const insertDemoLocations = async (connection, userId) => {
  const idByName = {};
  for (const name of DEFAULT_LOCATIONS) {
    const id = uuidv4();
    await connection.query(
      `INSERT INTO locations (id, user_id, name, description, created_at, updated_at)
       VALUES (?, ?, ?, NULL, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [id, userId, name]
    );
    idByName[name] = id;
  }
  return idByName;
};

const loadCategoryIdByName = async (connection) => {
  const [rows] = await connection.query('SELECT id, name FROM categories');
  if (rows.length === 0) {
    throw new Error('Tabela categories je prazna. Pokrenite "npm run db:create" pre "npm run seed".');
  }
  const idByName = {};
  for (const row of rows) {
    idByName[row.name] = row.id;
  }
  return idByName;
};

const insertItems = async (connection, userId, items) => {
  for (const item of items) {
    await connection.query(
      `INSERT INTO inventory_items (
         id, user_id, name, description, category_id, location_id,
         manufacturer, model, serial_number, quantity,
         purchase_price, estimated_value, currency,
         purchase_date, warranty_expiration_date, seller, notes,
         created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [
        item.id,
        userId,
        item.name,
        item.description,
        item.categoryId,
        item.locationId,
        item.manufacturer,
        item.model,
        item.serialNumber,
        item.quantity,
        item.purchasePrice,
        item.estimatedValue,
        item.currency,
        item.purchaseDate,
        item.warrantyExpirationDate,
        item.seller,
        item.notes,
      ]
    );
  }
};

async function seed() {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();

    await deleteExistingDemoUser(connection);
    const userId = await insertDemoUser(connection);
    const locationIdByName = await insertDemoLocations(connection, userId);
    const categoryIdByName = await loadCategoryIdByName(connection);

    const today = new Date();
    const locationIds = DEFAULT_LOCATIONS.map((name) => locationIdByName[name]);
    const items = buildDemoItems({ today, categoryIdByName, locationIds });
    await insertItems(connection, userId, items);

    await connection.commit();

    console.log(`Demo nalog "${DEMO_EMAIL}" je kreiran (lozinka: "${DEMO_PASSWORD}").`);
    console.log(`Ubačeno je ${items.length} predmeta u ${DEFAULT_LOCATIONS.length} lokacija.`);
  } catch (err) {
    await connection.rollback();
    throw err;
  } finally {
    connection.release();
  }
}

if (require.main === module) {
  seed()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('Seed nije uspeo:', err.message);
      process.exit(1);
    });
}

module.exports = seed;
