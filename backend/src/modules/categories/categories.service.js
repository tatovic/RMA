const { v4: uuidv4 } = require('uuid');

const pool = require('../../config/db');
const AppError = require('../../utils/AppError');

const findById = async (id) => {
  const [rows] = await pool.query('SELECT * FROM categories WHERE id = ? LIMIT 1', [id]);
  return rows[0];
};

const findByIdWithItemCount = async (id) => {
  const [rows] = await pool.query(
    `SELECT c.*, COUNT(i.id) AS item_count
     FROM categories c
     LEFT JOIN inventory_items i ON i.category_id = c.id AND i.deleted_at IS NULL
     WHERE c.id = ?
     GROUP BY c.id`,
    [id]
  );
  return rows[0];
};

// BR-003 — brojač je globalan, ne po korisniku: kategorije su zajedničke za sve.
const listCategories = async () => {
  const [rows] = await pool.query(
    `SELECT c.*, COUNT(i.id) AS item_count
     FROM categories c
     LEFT JOIN inventory_items i ON i.category_id = c.id AND i.deleted_at IS NULL
     GROUP BY c.id
     ORDER BY c.sort_order ASC`
  );
  return rows;
};

const createCategory = async (input) => {
  const id = uuidv4();

  try {
    await pool.query(
      `INSERT INTO categories (id, name, description, icon_key, sort_order, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [id, input.name, input.description ?? null, input.iconKey ?? null, input.sortOrder]
    );
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      throw new AppError('CATEGORY_NAME_TAKEN');
    }
    throw err;
  }

  return findByIdWithItemCount(id);
};

const updateCategory = async (id, input) => {
  const current = await findById(id);
  if (!current) {
    throw new AppError('NOT_FOUND');
  }

  try {
    await pool.query(
      `UPDATE categories SET name = ?, description = ?, icon_key = ?, sort_order = ?, updated_at = UTC_TIMESTAMP(3)
       WHERE id = ?`,
      [input.name, input.description ?? null, input.iconKey ?? null, input.sortOrder, id]
    );
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      throw new AppError('CATEGORY_NAME_TAKEN');
    }
    throw err;
  }

  return findByIdWithItemCount(id);
};

// BR-014 — kategorija u upotrebi (bar jedan neobrisan predmet bilo kog korisnika) se ne briše.
//
// Guard i brisanje idu u jednoj transakciji sa FOR UPDATE (tiket 28, nalaz A8), inače paralelan
// POST /api/items sme da upiše predmet u kategoriju koja se upravo briše. Baš zato što je ovaj
// brojač GLOBALAN (svi korisnici, BR-003) smemo da imamo ON DELETE CASCADE na fk_items_category:
// kaskada može da dohvati samo tombstone redove, nikad ničiji živ predmet (migracija 001).
const deleteCategory = async (id) => {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();

    const [currentRows] = await connection.query('SELECT id FROM categories WHERE id = ? LIMIT 1 FOR UPDATE', [id]);
    if (currentRows.length === 0) {
      throw new AppError('NOT_FOUND');
    }

    const [[{ itemCount }]] = await connection.query(
      'SELECT COUNT(*) AS itemCount FROM inventory_items WHERE category_id = ? AND deleted_at IS NULL FOR UPDATE',
      [id]
    );

    if (itemCount > 0) {
      throw new AppError('CATEGORY_IN_USE', { itemCount });
    }

    await connection.query('DELETE FROM categories WHERE id = ?', [id]);
    await connection.commit();
  } catch (err) {
    await connection.rollback();
    // Odbrana u dubinu — vidi isti komentar u locations.service.js (tiket 28, blokirajući nalaz 02).
    if (err.code === 'ER_ROW_IS_REFERENCED_2' || err.errno === 1451) {
      throw new AppError('CATEGORY_IN_USE', { itemCount: 0 });
    }
    throw err;
  } finally {
    connection.release();
  }
};

module.exports = { listCategories, createCategory, updateCategory, deleteCategory };
