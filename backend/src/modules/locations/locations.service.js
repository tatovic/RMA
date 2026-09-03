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
      // Sudar je mogao biti na PRIMARY KEY (id pripada TUĐOJ lokaciji, pa ga findOwnedById ne vidi)
      // ili na uq_locations_user_name. Ista razlika koju createItem već pravi kroz ITEM_ID_TAKEN —
      // bez ove provere bi klijent koji je slučajno pogodio tuđi UUID dobio poruku o zauzetom
      // nazivu i beskorisno preimenovao lokaciju (tiket 28, nalaz 07).
      const [idRows] = await pool.query('SELECT id FROM locations WHERE id = ? LIMIT 1', [input.id]);
      throw new AppError(idRows.length > 0 ? 'LOCATION_ID_TAKEN' : 'LOCATION_NAME_TAKEN');
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
//
// Prebrojavanje i brisanje idu u jednoj transakciji sa FOR UPDATE (tiket 28, nalaz A8): bez toga
// paralelan POST /api/items sme da upiše predmet između provere i DELETE-a, i predmet nestane sa
// lokacijom. Soft-obrisani predmeti se namerno ne broje — njih odnosi ON DELETE CASCADE (migracija
// 001), koja se zbog ovog istog guard-a nikad ne dohvata živog predmeta.
const deleteLocation = async (userId, id) => {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();

    const [currentRows] = await connection.query(
      'SELECT id FROM locations WHERE id = ? AND user_id = ? LIMIT 1 FOR UPDATE',
      [id, userId]
    );
    if (currentRows.length === 0) {
      throw new AppError('NOT_FOUND');
    }

    const [[{ itemCount }]] = await connection.query(
      'SELECT COUNT(*) AS itemCount FROM inventory_items WHERE location_id = ? AND deleted_at IS NULL FOR UPDATE',
      [id]
    );

    if (itemCount > 0) {
      throw new AppError('LOCATION_IN_USE', { itemCount });
    }

    await connection.query('DELETE FROM locations WHERE id = ? AND user_id = ?', [id, userId]);
    await connection.commit();
  } catch (err) {
    await connection.rollback();
    // Odbrana u dubinu: i sa transakcijom iznad, red koji strani ključ i dalje drži mora da izađe
    // kao 409 sa razumljivom porukom, a ne kao 500 (tiket 28, blokirajući nalaz 02).
    if (err.code === 'ER_ROW_IS_REFERENCED_2' || err.errno === 1451) {
      throw new AppError('LOCATION_IN_USE', { itemCount: 0 });
    }
    throw err;
  } finally {
    connection.release();
  }
};

module.exports = { listLocations, createLocation, updateLocation, deleteLocation };
