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

// DB-RULE-04 — starija klijentska verzija se odbija sa 409 i vraća se serverska verzija.
const updateItem = async (userId, id, input) => {
  const current = await findActiveOwnedById(userId, id);
  if (!current) {
    throw new AppError('NOT_FOUND');
  }

  await assertCategoryExists(input.categoryId);
  await assertLocationOwnedByUser(input.locationId, userId);

  const clientUpdatedAt = new Date(input.updatedAt).getTime();
  const serverUpdatedAt = new Date(current.updated_at).getTime();

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

// FR-098 — bez `since` vraća samo aktivne predmete; sa `since` vraća delta (uključujući obrisane).
const listItems = async (userId, since) => {
  const [[{ serverTime }]] = await pool.query('SELECT UTC_TIMESTAMP(3) AS serverTime');

  let rows;
  if (since) {
    [rows] = await pool.query(
      'SELECT * FROM inventory_items WHERE user_id = ? AND updated_at > ? ORDER BY updated_at ASC LIMIT 500',
      [userId, new Date(since)]
    );
  } else {
    [rows] = await pool.query(
      'SELECT * FROM inventory_items WHERE user_id = ? AND deleted_at IS NULL ORDER BY created_at DESC',
      [userId]
    );
  }

  return { items: rows, serverTime };
};

module.exports = { createItem, getItem, updateItem, deleteItem, listItems };
