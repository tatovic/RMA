const bcrypt = require('bcrypt');

const pool = require('../../config/db');
const env = require('../../config/env');
const AppError = require('../../utils/AppError');

const getById = async (userId) => {
  const [rows] = await pool.query('SELECT * FROM users WHERE id = ? LIMIT 1', [userId]);
  return rows[0];
};

const updateProfile = async (userId, { name, currency }) => {
  const fields = [];
  const values = [];

  if (name !== undefined) {
    fields.push('name = ?');
    values.push(name);
  }
  if (currency !== undefined) {
    fields.push('currency = ?');
    values.push(currency);
  }

  if (fields.length > 0) {
    fields.push('updated_at = UTC_TIMESTAMP(3)');
    values.push(userId);
    await pool.query(`UPDATE users SET ${fields.join(', ')} WHERE id = ?`, values);
  }

  return getById(userId);
};

const changePassword = async (userId, { currentPassword, newPassword }) => {
  const user = await getById(userId);

  const passwordMatches = await bcrypt.compare(currentPassword, user.password_hash);
  if (!passwordMatches) {
    throw new AppError('WRONG_CURRENT_PASSWORD');
  }

  const passwordHash = await bcrypt.hash(newPassword, env.BCRYPT_ROUNDS);
  // token_version + 1 poništava svaki token izdat pre ove promene (tiket 28, nalaz 12) — bez toga
  // je token ukraden pre promene lozinke ostajao važeći do isteka, punih sedam dana.
  await pool.query(
    `UPDATE users SET password_hash = ?, token_version = token_version + 1, updated_at = UTC_TIMESTAMP(3)
     WHERE id = ?`,
    [passwordHash, userId]
  );
};

module.exports = { getById, updateProfile, changePassword };
