const jwt = require('jsonwebtoken');

const pool = require('../config/db');
const env = require('../config/env');
const AppError = require('../utils/AppError');
const asyncHandler = require('../utils/asyncHandler');

// FR-014 — nalog se proverava u bazi pri svakom zahtevu, sadržaju tokena se ne veruje.
const authenticate = asyncHandler(async (req, res, next) => {
  const header = req.headers.authorization || '';
  const [scheme, token] = header.split(' ');

  if (scheme !== 'Bearer' || !token) {
    throw new AppError('TOKEN_INVALID');
  }

  let payload;
  try {
    payload = jwt.verify(token, env.JWT_SECRET);
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      throw new AppError('TOKEN_EXPIRED');
    }
    throw new AppError('TOKEN_INVALID');
  }

  const [rows] = await pool.query('SELECT * FROM users WHERE id = ? LIMIT 1', [payload.sub]);
  const user = rows[0];

  if (!user) {
    throw new AppError('TOKEN_INVALID');
  }

  if (!user.is_active) {
    throw new AppError('ACCOUNT_DEACTIVATED');
  }

  // Promena lozinke uvećava users.token_version, čime svaki ranije izdat token ostaje sa starim
  // `tv` claim-om i ovde pada kao nevažeći (tiket 28, nalaz 12). Token izdat pre uvođenja kolone
  // nema claim — takav se tretira kao verzija 0, što se poklapa sa DEFAULT 0 u šemi.
  if ((payload.tv ?? 0) !== user.token_version) {
    throw new AppError('TOKEN_INVALID');
  }

  // OWN-01 — narednim slojevima ide identifikator iz verifikovanog tokena, ne iz tela zahteva.
  req.userId = user.id;
  req.user = user;
  next();
});

module.exports = authenticate;
