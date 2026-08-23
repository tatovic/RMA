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
const deleteCategory = async (id) => {
  const current = await findById(id);
  if (!current) {
    throw new AppError('NOT_FOUND');
  }

  const [[{ itemCount }]] = await pool.query(
    'SELECT COUNT(*) AS itemCount FROM inventory_items WHERE category_id = ? AND deleted_at IS NULL',
    [id]
  );

  if (itemCount > 0) {
    throw new AppError('CATEGORY_IN_USE', { itemCount });
  }

  await pool.query('DELETE FROM categories WHERE id = ?', [id]);
};

module.exports = { listCategories, createCategory, updateCategory, deleteCategory };
