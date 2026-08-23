const pool = require('../../config/db');
const AppError = require('../../utils/AppError');

const findOwnedById = async (userId, id) => {
  const [rows] = await pool.query('SELECT * FROM locations WHERE id = ? AND user_id = ? LIMIT 1', [id, userId]);
  return rows[0];
};

const findOwnedByIdWithItemCount = async (userId, id) => {
  const [rows] = await pool.query(
    `SELECT l.*, COUNT(i.id) AS item_count
     FROM locations l
     LEFT JOIN inventory_items i ON i.location_id = l.id AND i.deleted_at IS NULL
     WHERE l.id = ? AND l.user_id = ?
     GROUP BY l.id`,
    [id, userId]
  );
  return rows[0];
};

const listLocations = async (userId) => {
  const [rows] = await pool.query(
    `SELECT l.*, COUNT(i.id) AS item_count
     FROM locations l
     LEFT JOIN inventory_items i ON i.location_id = l.id AND i.deleted_at IS NULL
     WHERE l.user_id = ?
     GROUP BY l.id
     ORDER BY l.name ASC`,
    [userId]
  );
  return rows;
};

// Idempotentno kreiranje: id generiše klijent, ponovno slanje istog id-ja vraća postojeću lokaciju.
const createLocation = async (userId, input) => {
  const existing = await findOwnedById(userId, input.id);
  if (existing) {
    return { location: await findOwnedByIdWithItemCount(userId, input.id), created: false };
  }

  try {
    await pool.query(
      `INSERT INTO locations (id, user_id, name, description, created_at, updated_at)
       VALUES (?, ?, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [input.id, userId, input.name, input.description ?? null]
    );
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      // Trka dva istovremena zahteva sa istim id-jem — druga provera posle sudara na PK.
      const racedLocation = await findOwnedById(userId, input.id);
      if (racedLocation) {
        return { location: await findOwnedByIdWithItemCount(userId, input.id), created: false };
      }
      throw new AppError('LOCATION_NAME_TAKEN');
    }
    throw err;
  }

  return { location: await findOwnedByIdWithItemCount(userId, input.id), created: true };
};

const updateLocation = async (userId, id, input) => {
  const current = await findOwnedById(userId, id);
  if (!current) {
    throw new AppError('NOT_FOUND');
  }

  try {
    await pool.query(
      `UPDATE locations SET name = ?, description = ?, updated_at = UTC_TIMESTAMP(3)
       WHERE id = ? AND user_id = ?`,
      [input.name, input.description ?? null, id, userId]
    );
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      throw new AppError('LOCATION_NAME_TAKEN');
    }
    throw err;
  }

  return findOwnedByIdWithItemCount(userId, id);
};

// BR-014 — lokacija u upotrebi (bar jedan neobrisan predmet) se ne briše.
const deleteLocation = async (userId, id) => {
  const current = await findOwnedById(userId, id);
  if (!current) {
    throw new AppError('NOT_FOUND');
  }

  const [[{ itemCount }]] = await pool.query(
    'SELECT COUNT(*) AS itemCount FROM inventory_items WHERE location_id = ? AND deleted_at IS NULL',
    [id]
  );

  if (itemCount > 0) {
    throw new AppError('LOCATION_IN_USE', { itemCount });
  }

  await pool.query('DELETE FROM locations WHERE id = ? AND user_id = ?', [id, userId]);
};

module.exports = { listLocations, createLocation, updateLocation, deleteLocation };
