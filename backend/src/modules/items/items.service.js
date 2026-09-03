const pool = require('../../config/db');
const AppError = require('../../utils/AppError');
const { serializeItem } = require('../../utils/serializer');

// OWN-04 / DB-RULE-01 — kategorija mora postojati.
const assertCategoryExists = async (categoryId) => {
  const [rows] = await pool.query('SELECT id FROM categories WHERE id = ? LIMIT 1', [categoryId]);
  if (rows.length === 0) {
    throw new AppError('NOT_FOUND');
  }
};

// OWN-04 / DB-RULE-01 — lokacija mora pripadati istom korisniku, inače 404 (ne 403).
const assertLocationOwnedByUser = async (locationId, userId) => {
  const [rows] = await pool.query('SELECT id FROM locations WHERE id = ? AND user_id = ? LIMIT 1', [
    locationId,
    userId,
  ]);
  if (rows.length === 0) {
    throw new AppError('NOT_FOUND');
  }
};

// Svaki upit nad inventory_items nosi user_id — OWN-01, OWN-03.
const findOwnedById = async (userId, id) => {
  const [rows] = await pool.query('SELECT * FROM inventory_items WHERE id = ? AND user_id = ? LIMIT 1', [
    id,
    userId,
  ]);
  return rows[0];
};

const findActiveOwnedById = async (userId, id) => {
  const [rows] = await pool.query(
    'SELECT * FROM inventory_items WHERE id = ? AND user_id = ? AND deleted_at IS NULL LIMIT 1',
    [id, userId]
  );
  return rows[0];
};

const itemValues = (input) => [
  input.name,
  input.description ?? null,
  input.categoryId,
  input.locationId,
  input.manufacturer ?? null,
  input.model ?? null,
  input.serialNumber ?? null,
  input.quantity,
  input.purchasePrice ?? null,
  input.estimatedValue ?? null,
  input.currency,
  input.purchaseDate ?? null,
  input.warrantyExpirationDate ?? null,
  input.seller ?? null,
  input.notes ?? null,
];

// Idempotentno kreiranje: id generiše klijent, ponovno slanje istog id-ja vraća postojeći predmet.
const createItem = async (userId, input) => {
  const existing = await findOwnedById(userId, input.id);
  if (existing) {
    return { item: existing, created: false };
  }

  await assertCategoryExists(input.categoryId);
  await assertLocationOwnedByUser(input.locationId, userId);

  try {
    await pool.query(
      `INSERT INTO inventory_items (
         id, user_id, name, description, category_id, location_id,
         manufacturer, model, serial_number, quantity,
         purchase_price, estimated_value, currency,
         purchase_date, warranty_expiration_date, seller, notes,
         created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [input.id, userId, ...itemValues(input)]
    );
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      // Trka dva istovremena zahteva sa istim id-jem — druga provera posle sudara na PK.
      const racedItem = await findOwnedById(userId, input.id);
      if (racedItem) {
        return { item: racedItem, created: false };
      }
      throw new AppError('ITEM_ID_TAKEN');
    }
    throw err;
  }

  const item = await findOwnedById(userId, input.id);
  return { item, created: true };
};

const getItem = async (userId, id) => {
  const item = await findActiveOwnedById(userId, id);
  if (!item) {
    // OWN-03 — tuđ ili nepostojeći predmet, uvek 404.
    throw new AppError('NOT_FOUND');
  }
  return item;
};

// Koliko `updatedAt` sme da bude ispred serverskog vremena pre nego što ga odbijemo kao neispravan.
const MAX_CLOCK_SKEW_MS = 24 * 60 * 60 * 1000;

// DB-RULE-04 — starija klijentska verzija se odbija sa 409 i vraća se serverska verzija.
//
// `updatedAt` koji klijent šalje je verzija koju je server poslednju izdao za taj red — neproziran
// token, ne vreme sa telefona (db.md DB-RULE-04, ispravljeno u tiketu 28). Poređenje ispod zato
// ostaje isto; jedina dopuna je odbijanje vrednosti koja je toliko u budućnosti da je očigledno
// nastala na pokvarenom satu. Bez toga bi takav klijent dobijao svaki konflikt, tiho pregazivši
// izmene sa ostalih uređaja umesto da glasno padne.
const updateItem = async (userId, id, input) => {
  const current = await findActiveOwnedById(userId, id);
  if (!current) {
    throw new AppError('NOT_FOUND');
  }

  await assertCategoryExists(input.categoryId);
  await assertLocationOwnedByUser(input.locationId, userId);

  const clientUpdatedAt = new Date(input.updatedAt).getTime();
  const serverUpdatedAt = new Date(current.updated_at).getTime();

  const [[{ now }]] = await pool.query('SELECT UTC_TIMESTAMP(3) AS now');
  if (clientUpdatedAt - new Date(now).getTime() > MAX_CLOCK_SKEW_MS) {
    throw new AppError('VALIDATION_ERROR', [
      {
        field: 'updatedAt',
        message: 'updatedAt je previše u budućnosti — proverite podešavanje vremena na uređaju',
      },
    ]);
  }

  if (clientUpdatedAt < serverUpdatedAt) {
    throw new AppError('SYNC_CONFLICT', serializeItem(current));
  }

  await pool.query(
    `UPDATE inventory_items SET
       name = ?, description = ?, category_id = ?, location_id = ?,
       manufacturer = ?, model = ?, serial_number = ?, quantity = ?,
       purchase_price = ?, estimated_value = ?, currency = ?,
       purchase_date = ?, warranty_expiration_date = ?, seller = ?, notes = ?,
       updated_at = UTC_TIMESTAMP(3)
     WHERE id = ? AND user_id = ?`,
    [...itemValues(input), id, userId]
  );

  return findActiveOwnedById(userId, id);
};

// Soft delete (FR-026) — briše se markiranjem, ne redom; ponovljeno brisanje ostaje uspešno.
const deleteItem = async (userId, id) => {
  const item = await findOwnedById(userId, id);
  if (!item) {
    throw new AppError('NOT_FOUND');
  }

  if (item.deleted_at) {
    return;
  }

  await pool.query(
    'UPDATE inventory_items SET deleted_at = UTC_TIMESTAMP(3), updated_at = UTC_TIMESTAMP(3) WHERE id = ? AND user_id = ?',
    [id, userId]
  );
};

// Veličina jedne strane delte. Dohvata se PAGE_SIZE + 1 red da bi se `hasMore` znao bez drugog upita.
const DELTA_PAGE_SIZE = 500;

// FR-098 — bez `since` vraća samo aktivne predmete; sa `since` vraća delta (uključujući obrisane).
//
// Delta je straničena (tiket 28, blokirajući nalaz 01). Ranije je vraćala `LIMIT 500` bez ikakvog
// signala da ima još — klijent bi upisao serverTime kao novi `since` i time trajno preskočio svaki
// red preko petstotog. Odgovor sada nosi `nextSince` (dokle je klijent stvarno stigao) i `hasMore`.
const listItems = async (userId, since) => {
  const [[{ serverTime }]] = await pool.query('SELECT UTC_TIMESTAMP(3) AS serverTime');

  if (!since) {
    // Prvi pull svežeg klijenta. Namerno bez straničenja: ova grana isključuje tombstone redove i
    // vraća samo živ inventar jednog korisnika (procena iz db.md sekcija 12 — reda veličine stotina
    // predmeta, ~300 KB), pa jedan odgovor nosi ceo skup i klijent nema šta da nastavi.
    const [rows] = await pool.query(
      'SELECT * FROM inventory_items WHERE user_id = ? AND deleted_at IS NULL ORDER BY created_at DESC',
      [userId]
    );
    return { items: rows, serverTime, nextSince: serverTime, hasMore: false };
  }

  // (updated_at, id) kao sortiranje daje stabilan redosled i kad više redova deli isti milisekundni
  // updated_at — bez `id` u ORDER BY, MySQL sme da ih vrati različitim redosledom između strana.
  const [rows] = await pool.query(
    'SELECT * FROM inventory_items WHERE user_id = ? AND updated_at > ? ORDER BY updated_at ASC, id ASC LIMIT ?',
    [userId, new Date(since), DELTA_PAGE_SIZE + 1]
  );

  if (rows.length <= DELTA_PAGE_SIZE) {
    // Delta je iscrpljena — `since` sledećeg pull-a je serversko vreme ovog odgovora.
    return { items: rows, serverTime, nextSince: serverTime, hasMore: false };
  }

  let page = rows.slice(0, DELTA_PAGE_SIZE);

  // `since` je poređenje po vremenu (updated_at > ?), pa granica strane ne sme da preseče grupu
  // redova sa istim updated_at: sledeći zahtev bi počeo OD te vrednosti i preskočio braću iz iste
  // milisekunde koja su ostala iza reza. Rep sa poslednjim updated_at zato ide u sledeću stranu.
  const lastUpdatedAt = page[page.length - 1].updated_at.getTime();
  const trimmed = page.filter((row) => row.updated_at.getTime() !== lastUpdatedAt);

  if (trimmed.length === 0) {
    // Više od DELTA_PAGE_SIZE redova deli istu milisekundu — rez bi ispraznio stranu i sync bi stao
    // u mestu. Tada se strana vraća cela: `nextSince` je ta ista milisekunda, pa sledeći zahtev
    // preskače ovu grupu. Jedini put na kojem se red može propustiti, i jedini kod kojeg je
    // alternativa (beskonačna petlja) gora.
    return { items: page, serverTime, nextSince: page[page.length - 1].updated_at, hasMore: true };
  }

  page = trimmed;
  return { items: page, serverTime, nextSince: page[page.length - 1].updated_at, hasMore: true };
};

module.exports = { createItem, getItem, updateItem, deleteItem, listItems, DELTA_PAGE_SIZE };
