# 02: MySQL šema i globalne kategorije

**What to build:** Baza podataka postoji i sprovodi pravila sama od sebe. Tabele, relacije, indeksi i ograničenja su na mestu, a sistem ima svojih jedanaest globalnih kategorija koje će svi korisnici deliti. Šema se može obrisati i ponovo napraviti jednom komandom tokom razvoja.

**Blocked by:** 01 — Repo skelet i Express server.

**Status:** ready-for-agent

- [x] Sve četiri tabele iz `db.md` sekcija 3 postoje sa tačnim tipovima kolona
- [x] Novac je `BIGINT` u minor jedinicama, kalendarski datumi su `DATE`, trenuci su `DATETIME(3)` u UTC
- [ ] Svih sedam indeksa iz `db.md` postoji i vidljivi su kroz `SHOW INDEX`
- [x] Jedinstvenost email adrese je sprovedena na nivou baze
- [x] Jedinstvenost naziva lokacije važi po korisniku, ne globalno — dva korisnika mogu imati „Kuhinja"
- [x] Strani ključ ka kategoriji i lokaciji je `RESTRICT` i stvarno blokira brisanje entiteta u upotrebi
- [x] `CHECK` ograničenja odbijaju količinu manju od 1 i negativnu cenu
- [ ] Jedanaest kategorija iz `db.md` sekcija 4.1 je ubačeno sa ispravnim redosledom i ključem ikonice
- [ ] Komanda za kreiranje baze je idempotentna — dvostruko pokretanje ne prijavljuje grešku
- [x] Postoji komanda za potpuni reset baze, jasno označena kao razvojna

**Napomena:** `backend/src/db/schema.sql` (idempotentan DDL, `CREATE TABLE IF NOT EXISTS`), `backend/src/db/categories.js` (11 kategorija), `backend/src/db/createDatabase.js` (`npm run db:create`) i `backend/src/db/resetDatabase.js` (`npm run db:reset`, blokiran u produkciji) su implementirani i statički provereni prema `db.md`. Stavke koje zahtevaju **stvarno izvršavanje** nad lokalnom MySQL bazom (`SHOW INDEX`, dvostruko pokretanje `db:create`, upis 11 kategorija) nisu mogle biti provere­ne u ovoj sesiji — root lozinka za lokalni `MySQL80` servis nije prihvaćena (probane vrednosti: prazno, `1234567`, `root123`). Korisnik je odlučio da se nastavi bez uživo testa. Kad tačna lozinka bude poznata, pokrenuti `npm run db:create` (dvaput, radi provere idempotencije) i `SHOW INDEX FROM inventory_items` pa doštiklirati preostale stavke.
