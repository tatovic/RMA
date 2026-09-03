// Kredencijali test admina — dele ih run.js (koji ga upisuje u bazu) i helpers.js (koji se njime
// prijavljuje). Izdvojeni u zaseban fajl da run.js ne bi morao da učita helpers.js, koji povlači
// ceo Express app i pool konekcija još pre nego što test baza postoji.
module.exports = {
  ADMIN_EMAIL: 'test-admin@example.com',
  ADMIN_PASSWORD: 'AdminLozinka123',
};
