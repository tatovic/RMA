const pool = require('../../config/db');
const AppError = require('../../utils/AppError');

// OWN-06 — administratorska statistika daje samo agregatne brojeve, nikad sadržaj tuđeg inventara.
const getStats = async () => {
  const [[userStats]] = await pool.query(
    `SELECT
       COUNT(*) AS registeredUsers,
       COALESCE(SUM(is_active = 1), 0) AS activeUsers,
       COALESCE(SUM(is_active = 0), 0) AS deactivatedUsers
     FROM users`
  );

  const [[{ totalItems }]] = await pool.query(
    'SELECT COUNT(*) AS totalItems FROM inventory_items WHERE deleted_at IS NULL'
  );

  const [[{ totalCategories }]] = await pool.query('SELECT COUNT(*) AS totalCategories FROM categories');

  return {
    registeredUsers: Number(userStats.registeredUsers),
    activeUsers: Number(userStats.activeUsers),
    deactivatedUsers: Number(userStats.deactivatedUsers),
    totalItems: Number(totalItems),
    totalCategories: Number(totalCategories),
  };
};

// OWN-06 — lista korisnika izlaže samo brojač predmeta, nikad njihov sadržaj.
const listUsers = async () => {
  const [rows] = await pool.query(
    `SELECT u.*, COUNT(i.id) AS item_count
     FROM users u
     LEFT JOIN inventory_items i ON i.user_id = u.id AND i.deleted_at IS NULL
     GROUP BY u.id
     ORDER BY u.created_at ASC`
  );
  return rows;
};

const findById = async (id) => {
  const [rows] = await pool.query('SELECT * FROM users WHERE id = ? LIMIT 1', [id]);
  return rows[0];
};

const findByIdWithItemCount = async (id) => {
  const [rows] = await pool.query(
    `SELECT u.*, COUNT(i.id) AS item_count
     FROM users u
     LEFT JOIN inventory_items i ON i.user_id = u.id AND i.deleted_at IS NULL
     WHERE u.id = ?
     GROUP BY u.id`,
    [id]
  );
  return rows[0];
};

// BR-004 — admin ne može deaktivirati sopstveni nalog.
const updateUserStatus = async (adminUserId, targetUserId, isActive) => {
  const user = await findById(targetUserId);
  if (!user) {
    throw new AppError('NOT_FOUND');
  }

  if (targetUserId === adminUserId && !isActive) {
    throw new AppError('CANNOT_DEACTIVATE_SELF');
  }

  await pool.query('UPDATE users SET is_active = ?, updated_at = UTC_TIMESTAMP(3) WHERE id = ?', [
    isActive ? 1 : 0,
    targetUserId,
  ]);

  return findByIdWithItemCount(targetUserId);
};

module.exports = { getStats, listUsers, updateUserStatus };
