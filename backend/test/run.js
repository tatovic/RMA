const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

// Pokretač test suite-a (tiket 28, nalaz 15). Postoji iz tri razloga:
//  1. NODE_ENV i DB_NAME moraju biti postavljeni PRE nego što ijedan test učita config/env.js —
//     dotenv ne pregazi promenljive koje već postoje u okruženju, pa ovo pouzdano preusmerava
//     testove na zasebnu bazu. Bez toga bi `npm test` obrisao razvojnu bazu.
//  2. Sintaksa za postavljanje promenljive okruženja u npm skripti nije ista na Windows-u i
//     POSIX-u; JS pokretač radi svuda bez dodatne zavisnosti (cross-env).
//  3. Kreiranje i brisanje test baze su globalni korak — `node --test` svaki fajl pokreće u svom
//     procesu, pa nema zajedničkog before/after.

const TEST_DB_NAME = process.env.TEST_DB_NAME || 'home_inventory_test';

process.env.NODE_ENV = 'test';
process.env.DB_NAME = TEST_DB_NAME;

const env = require('../src/config/env');
const { createDatabase } = require('../src/db/createDatabase');
const mysql = require('mysql2/promise');
const { quoteIdentifier } = require('../src/db/createDatabase');

const adminConnection = () =>
  mysql.createConnection({
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USER,
    password: env.DB_PASSWORD,
    multipleStatements: true,
  });

async function dropTestDatabase() {
  const connection = await adminConnection();
  try {
    await connection.query(`DROP DATABASE IF EXISTS ${quoteIdentifier(env.DB_NAME)}`);
  } finally {
    await connection.end();
  }
}

// BR-001 — prvi korisnik u bazi postaje ADMIN. Admin se zato pravi ovde, pre svih testova, da
// nijedna registracija iz testa ne bi slučajno dobila ADMIN rolu i tako menjala značenje 403 provera.
async function seedAdmin() {
  const bcrypt = require('bcrypt');
  const { v4: uuidv4 } = require('uuid');
  const { ADMIN_EMAIL, ADMIN_PASSWORD } = require('./helpers.constants');

  const connection = await mysql.createConnection({
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USER,
    password: env.DB_PASSWORD,
    database: env.DB_NAME,
  });

  try {
    const passwordHash = await bcrypt.hash(ADMIN_PASSWORD, env.BCRYPT_ROUNDS);
    await connection.query(
      `INSERT INTO users (id, name, email, password_hash, role, is_active, currency, token_version, created_at, updated_at)
       VALUES (?, 'Test Admin', ?, ?, 'ADMIN', 1, 'RSD', 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [uuidv4(), ADMIN_EMAIL, passwordHash]
    );
  } finally {
    await connection.end();
  }
}

function runTests() {
  return new Promise((resolve) => {
    // Fajlovi se nabrajaju eksplicitno umesto da se prosledi direktorijum — u istom folderu žive i
    // run.js i helpers.js, koje test runner ne sme da pokuša da pokrene kao testove.
    const testFiles = fs
      .readdirSync(__dirname)
      .filter((name) => name.endsWith('.test.js'))
      .sort()
      .map((name) => path.join(__dirname, name));

    const child = spawn(
      process.execPath,
      // --test-concurrency=1: testovi dele jednu MySQL bazu, pa paralelni fajlovi umeju da se
      // međusobno vide (globalni brojači kategorija, admin lista korisnika).
      ['--test', '--test-concurrency=1', ...testFiles],
      { stdio: 'inherit', env: process.env }
    );
    child.on('exit', (code) => resolve(code ?? 1));
  });
}

async function main() {
  if (env.DB_NAME === 'home_inventory') {
    throw new Error('Test baza ne sme da se zove "home_inventory" — suite je briše na kraju.');
  }

  console.log(`Pripremam test bazu "${env.DB_NAME}"...`);
  await dropTestDatabase();
  await createDatabase();
  await seedAdmin();

  const code = await runTests();

  console.log(`Brišem test bazu "${env.DB_NAME}"...`);
  await dropTestDatabase();

  process.exit(code);
}

main().catch((err) => {
  console.error('Test suite nije mogao da se pokrene:', err.message);
  process.exit(1);
});
