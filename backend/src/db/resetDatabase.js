const mysql = require('mysql2/promise');

const env = require('../config/env');
const { createDatabase, quoteIdentifier } = require('./createDatabase');

// RAZVOJNA KOMANDA — briše bazu u celosti. Ne sme se pokrenuti u produkciji.
async function resetDatabase() {
  if (env.NODE_ENV === 'production') {
    throw new Error('db:reset je razvojna komanda i ne sme se pokretati kada je NODE_ENV=production.');
  }

  const connection = await mysql.createConnection({
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USER,
    password: env.DB_PASSWORD,
  });

  try {
    console.warn(`[RAZVOJ] Brišem bazu "${env.DB_NAME}" u celosti...`);
    await connection.query(`DROP DATABASE IF EXISTS ${quoteIdentifier(env.DB_NAME)}`);
    console.log(`Baza "${env.DB_NAME}" je obrisana.`);
  } finally {
    await connection.end();
  }

  await createDatabase();
}

if (require.main === module) {
  resetDatabase()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('Resetovanje baze nije uspelo:', err.message);
      process.exit(1);
    });
}

module.exports = resetDatabase;
