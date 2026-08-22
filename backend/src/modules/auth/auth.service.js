const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const pool = require('../../config/db');
const env = require('../../config/env');
const AppError = require('../../utils/AppError');
const DEFAULT_LOCATIONS = require('../../db/locations');

const signToken = (user) =>
  jwt.sign({ sub: user.id, role: user.role }, env.JWT_SECRET, {
    expiresIn: env.JWT_EXPIRES_IN,
  });

// Registracija: BR-001 (rola po redosledu prijave) i BR-015 (devet lokacija) u jednoj transakciji.
const register = async ({ name, email, password }) => {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();

    const [existing] = await connection.query('SELECT id FROM users WHERE email = ? LIMIT 1', [email]);
    if (existing.length > 0) {
      throw new AppError('EMAIL_ALREADY_EXISTS');
    }

    const [countRows] = await connection.query('SELECT COUNT(*) AS count FROM users');
    const role = countRows[0].count === 0 ? 'ADMIN' : 'USER';

    const passwordHash = await bcrypt.hash(password, env.BCRYPT_ROUNDS);
    const userId = uuidv4();

    try {
      await connection.query(
        `INSERT INTO users (id, name, email, password_hash, role, is_active, currency, created_at, updated_at)
         VALUES (?, ?, ?, ?, ?, 1, 'RSD', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
        [userId, name, email, passwordHash, role]
      );
    } catch (err) {
      if (err.code === 'ER_DUP_ENTRY') {
        throw new AppError('EMAIL_ALREADY_EXISTS');
      }
      throw err;
    }

    for (const locationName of DEFAULT_LOCATIONS) {
      await connection.query(
        `INSERT INTO locations (id, user_id, name, description, created_at, updated_at)
         VALUES (?, ?, ?, NULL, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
        [uuidv4(), userId, locationName]
      );
    }

    const [userRows] = await connection.query('SELECT * FROM users WHERE id = ?', [userId]);
    const user = userRows[0];

    await connection.commit();

    return { user, token: signToken(user) };
  } catch (err) {
    await connection.rollback();
    throw err;
  } finally {
    connection.release();
  }
};

// FR-017 — ista poruka za nepostojeći email i pogrešnu lozinku.
const login = async ({ email, password }) => {
  const [rows] = await pool.query('SELECT * FROM users WHERE email = ? LIMIT 1', [email]);
  const user = rows[0];

  if (!user) {
    throw new AppError('INVALID_CREDENTIALS');
  }

  const passwordMatches = await bcrypt.compare(password, user.password_hash);
  if (!passwordMatches) {
    throw new AppError('INVALID_CREDENTIALS');
  }

  if (!user.is_active) {
    throw new AppError('ACCOUNT_DEACTIVATED');
  }

  return { user, token: signToken(user) };
};

module.exports = { register, login };
