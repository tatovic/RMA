const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const pool = require('../../config/db');
const env = require('../../config/env');
const AppError = require('../../utils/AppError');
const DEFAULT_LOCATIONS = require('../../db/locations');

// `tv` (token_version) poništava sve ranije izdate tokene kad korisnik promeni lozinku —
// authenticate.js poredi claim sa kolonom users.token_version (tiket 28, nalaz 12).
const signToken = (user) =>
  jwt.sign({ sub: user.id, role: user.role, tv: user.token_version ?? 0 }, env.JWT_SECRET, {
    expiresIn: env.JWT_EXPIRES_IN,
  });

// FR-017 traži da nepostojeći email i pogrešna lozinka budu nerazlučivi. Sama poruka je odavno ista,
// ali je vreme odgovora odavalo razliku: promašaj je vraćao odmah, pogodak posle ~100ms bcrypt-a.
// Zato se i na promašaju uporedi jedan fiksan hash — isti posao, isto trajanje (tiket 28, nalaz 12).
// Hash odgovara lozinci koja se nigde ne koristi; njegova vrednost je nebitna, samo trošak poređenja.
const DUMMY_PASSWORD_HASH = '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

// Registracija: BR-001 (rola po redosledu prijave) i BR-015 (devet lokacija) u jednoj transakciji.
const register = async ({ name, email, password }) => {
  const connection = await pool.getConnection();

  try {
    await connection.beginTransaction();

    const [existing] = await connection.query('SELECT id FROM users WHERE email = ? LIMIT 1', [email]);
    if (existing.length > 0) {
      throw new AppError('EMAIL_ALREADY_EXISTS');
    }

    // BR-001 — prvi registrovan korisnik postaje ADMIN. Običan COUNT(*) ne zaključava ništa, pa su
    // dve istovremene registracije nad praznom tabelom obe videle nulu i obe postale ADMIN. `FOR
    // UPDATE` nad praznim opsegom tera InnoDB na gap lock, koji drugu transakciju zadržava dok se
    // prva ne commit-uje (tiket 28, nalaz A6).
    const [existingUsers] = await connection.query('SELECT id FROM users LIMIT 1 FOR UPDATE');
    const role = existingUsers.length === 0 ? 'ADMIN' : 'USER';

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
    // Namerno poređenje sa fiksnim hash-om: izjednačava trajanje promašaja sa trajanjem pogotka.
    await bcrypt.compare(password, DUMMY_PASSWORD_HASH);
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

module.exports = { register, login, signToken };
