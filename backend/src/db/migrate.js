const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

const env = require('../config/env');

// db.md sekcija 11 — minimalan migracioni alat (tiket 28). schema.sql je pisan idempotentno
// (CREATE TABLE IF NOT EXISTS), pa izmena postojece kolone ili stranog kljuca kroz njega ne stize ni
// do jedne vec kreirane baze. Svaka takva izmena zato ide i kao numerisan fajl u migrations/.
//
// Pravila: fajl se primenjuje tacno jednom (evidencija je u tabeli schema_migrations), fajlovi se
// izvrsavaju po nazivu rastuce, a jednom commit-ovan fajl se vise ne menja — ispravka je nov fajl.
const MIGRATIONS_DIR = path.resolve(__dirname, 'migrations');

const readMigrationNames = () =>
  fs
    .readdirSync(MIGRATIONS_DIR)
    .filter((name) => name.endsWith('.sql'))
    .sort();

async function migrate() {
  const connection = await mysql.createConnection({
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USER,
    password: env.DB_PASSWORD,
    database: env.DB_NAME,
    multipleStatements: true,
    timezone: 'Z',
  });

  try {
    await connection.query(
      `CREATE TABLE IF NOT EXISTS schema_migrations (
         name       VARCHAR(255) NOT NULL,
         applied_at DATETIME(3)  NOT NULL,
         PRIMARY KEY (name)
       ) ENGINE=InnoDB`
    );

    const [appliedRows] = await connection.query('SELECT name FROM schema_migrations');
    const applied = new Set(appliedRows.map((row) => row.name));

    const pending = readMigrationNames().filter((name) => !applied.has(name));
    if (pending.length === 0) {
      console.log('Nema migracija koje čekaju — baza je na poslednjoj verziji.');
      return;
    }

    for (const name of pending) {
      const sql = fs.readFileSync(path.join(MIGRATIONS_DIR, name), 'utf8');
      // ALTER TABLE je DDL i u MySQL-u implicitno commit-uje, pa transakcija oko fajla ne bi ni
      // stitila — evidencija se zato upisuje odmah posle uspesnog izvrsenja.
      await connection.query(sql);
      await connection.query('INSERT INTO schema_migrations (name, applied_at) VALUES (?, UTC_TIMESTAMP(3))', [
        name,
      ]);
      console.log(`Primenjena migracija: ${name}`);
    }

    console.log(`Gotovo — primenjeno ${pending.length} migracija nad bazom "${env.DB_NAME}".`);
  } finally {
    await connection.end();
  }
}

// Sveze kreirana baza vec ima sve iz schema.sql, pa se migracije samo evidentiraju kao primenjene
// (bez izvrsavanja) — inace bi 001 pokusao da doda strani kljuc koji vec postoji.
async function markAllApplied(connection) {
  await connection.query(
    `CREATE TABLE IF NOT EXISTS schema_migrations (
       name       VARCHAR(255) NOT NULL,
       applied_at DATETIME(3)  NOT NULL,
       PRIMARY KEY (name)
     ) ENGINE=InnoDB`
  );
  for (const name of readMigrationNames()) {
    await connection.query(
      'INSERT IGNORE INTO schema_migrations (name, applied_at) VALUES (?, UTC_TIMESTAMP(3))',
      [name]
    );
  }
}

if (require.main === module) {
  migrate()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('Migracija nije uspela:', err.message);
      process.exit(1);
    });
}

module.exports = { migrate, markAllApplied, readMigrationNames };
