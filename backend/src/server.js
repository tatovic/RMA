const env = require('./config/env');
const app = require('./app');
const pool = require('./config/db');

app.listen(env.PORT, () => {
  console.log(`Server sluša na portu ${env.PORT} (${env.NODE_ENV})`);
});

pool
  .getConnection()
  .then((connection) => {
    connection.release();
    console.log(`Povezan na MySQL bazu "${env.DB_NAME}" (${env.DB_HOST}:${env.DB_PORT})`);
  })
  .catch((err) => {
    console.error(`Ne mogu da se povežem na MySQL bazu: ${err.message}`);
  });
