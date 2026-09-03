const crypto = require('crypto');
const request = require('supertest');

const app = require('../src/app');
const pool = require('../src/config/db');

// Zajedničke pomoćne funkcije za test suite (tiket 28, nalaz 15). Ništa ovde ne dira produkcionu
// bazu: run.js pre pokretanja podigne zasebnu bazu čije ime dolazi iz DB_NAME i obriše je na kraju.

const uuid = () => crypto.randomUUID();

// Svaki test pravi svoje naloge — deljeno stanje između testova je najčešći izvor lažnih padova.
const uniqueEmail = (prefix = 'test') => `${prefix}-${crypto.randomBytes(6).toString('hex')}@example.com`;

const api = () => request(app);

async function registerUser({ name = 'Test Korisnik', email = uniqueEmail(), password = 'Lozinka123' } = {}) {
  const response = await api()
    .post('/api/auth/register')
    .send({ name, email, password, confirmPassword: password })
    .expect(201);

  return { ...response.body, password, email };
}

async function loginUser(email, password) {
  const response = await api().post('/api/auth/login').send({ email, password }).expect(200);
  return response.body;
}

// Admin nalog pravi run.js pre svih testova (prvi korisnik u bazi = ADMIN, BR-001), pa nijedna
// registracija iz testa ne može slučajno da dobije ADMIN rolu.
const { ADMIN_EMAIL, ADMIN_PASSWORD } = require('./helpers.constants');

const loginAdmin = () => loginUser(ADMIN_EMAIL, ADMIN_PASSWORD);

const authHeader = (token) => ({ Authorization: `Bearer ${token}` });

async function firstCategoryId(token) {
  const response = await api().get('/api/categories').set(authHeader(token)).expect(200);
  return response.body.categories[0].id;
}

async function firstLocationId(token) {
  const response = await api().get('/api/locations').set(authHeader(token)).expect(200);
  return response.body.locations[0].id;
}

// Minimalno telo predmeta — samo obavezna polja iz items.schema.js.
const itemPayload = ({ id = uuid(), categoryId, locationId, name = 'Test predmet', ...rest } = {}) => ({
  id,
  name,
  categoryId,
  locationId,
  quantity: 1,
  currency: 'RSD',
  ...rest,
});

async function createItem(token, payload) {
  const response = await api().post('/api/items').set(authHeader(token)).send(payload).expect(201);
  return response.body;
}

// Korisnik sa registrovanim nalogom, tokenom, kategorijom i lokacijom — polazna tačka većine testova.
async function seededUser(prefix = 'user') {
  const user = await registerUser({ email: uniqueEmail(prefix) });
  const [categoryId, locationId] = await Promise.all([
    firstCategoryId(user.token),
    firstLocationId(user.token),
  ]);
  return { ...user, categoryId, locationId };
}

const closePool = () => pool.end();

module.exports = {
  api,
  app,
  pool,
  uuid,
  uniqueEmail,
  registerUser,
  loginUser,
  loginAdmin,
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  authHeader,
  firstCategoryId,
  firstLocationId,
  itemPayload,
  createItem,
  seededUser,
  closePool,
};
