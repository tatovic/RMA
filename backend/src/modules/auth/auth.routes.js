const { Router } = require('express');
const rateLimit = require('express-rate-limit');

const env = require('../../config/env');
const validate = require('../../middleware/validate');
const AppError = require('../../utils/AppError');
const { registerSchema, loginSchema } = require('./auth.schema');
const authController = require('./auth.controller');

// Neautentifikovane rute su jedine koje neko može gađati bez ijednog kredencijala, pa su i jedine
// kojima treba ograničenje broja pokušaja (tiket 28, nalaz 12). Bez njega je pogađanje lozinke
// ograničeno samo brzinom bcrypt-a, a registracija je otvorena za pravljenje naloga u petlji.
//
// Ograničenje se prekoračenjem prijavljuje kroz isti katalog grešaka kao sve ostalo (prd.md sekcija
// 10) — klijent ne sme da dobije podrazumevani HTML/plaintext odgovor biblioteke. VALIDATION_ERROR
// je najbliži postojeći kod; uvođenje novog bi tražilo i izmenu Android kataloga (ErrorCode.kt).
const tooManyRequests = (message) => (req, res, next) =>
  next(new AppError('VALIDATION_ERROR', [{ field: 'general', message }]));

// Automatizovani testovi (npm test) gađaju /login desetinama puta iz istog procesa — brojač bi ih
// oborio, a ono što proveravaju nije ograničenje nego ponašanje autentifikacije.
const skipInTests = () => env.NODE_ENV === 'test';

// Prijava — strogo: pogađanje lozinke je ovde jedini realan napad.
const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 10,
  standardHeaders: true,
  legacyHeaders: false,
  skip: skipInTests,
  handler: tooManyRequests('Previše pokušaja prijave. Pokušajte ponovo za nekoliko minuta.'),
});

// Registracija — labavije: legitiman korisnik je pošalje jednom, ali sporija ruka (bcrypt po
// zahtevu) i dalje ne sme da bude besplatna.
const registerLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  limit: 20,
  standardHeaders: true,
  legacyHeaders: false,
  skip: skipInTests,
  handler: tooManyRequests('Previše pokušaja registracije. Pokušajte ponovo kasnije.'),
});

const router = Router();

router.post('/register', registerLimiter, validate(registerSchema), authController.register);
router.post('/login', loginLimiter, validate(loginSchema), authController.login);

module.exports = router;
