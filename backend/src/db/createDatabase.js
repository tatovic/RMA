const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');
const { v4: uuidv4 } = require('uuid');

const env = require('../config/env');
const CATEGORIES = require('./categories');

async function seedCategories(connection) {
  for (const category of CATEGORIES) {
    // INSERT IGNORE oslanja se na uq_categories_name — drugo pokretanje ne dira postojeće redove.
    await connection.query(
      `INSERT IGNORE INTO categories (id, name, description, icon_key, sort_order, created_at, updated_at)
       VALUES (?, ?, NULL, ?, ?, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))`,
      [uuidv4(), category.name, category.iconKey, category.sortOrder]
    );
  }
}

async function createDatabase() {
  const connection = await mysql.createConnection({
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USER,
    password: env.DB_PASSWORD,
    multipleStatements: true,
    timezone: 'Z',
  });

  try {
    const schemaSql = fs.readFileSync(path.resolve(__dirname, 'schema.sql'), 'utf8');
    await connection.query(schemaSql);
    console.log(`Šema baze "${env.DB_NAME}" je na mestu.`);

    await seedCategories(connection);
    console.log('Jedanaest globalnih kategorija je na mestu.');
  } finally {
    await connection.end();
  }
}

if (require.main === module) {
  createDatabase()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('Kreiranje baze nije uspelo:', err.message);
      process.exit(1);
    });
}

module.exports = { createDatabase, seedCategories };
