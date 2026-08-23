# 02: MySQL šema i globalne kategorije

**What to build:** Baza podataka postoji i sprovodi pravila sama od sebe. Tabele, relacije, indeksi i ograničenja su na mestu, a sistem ima svojih jedanaest globalnih kategorija koje će svi korisnici deliti. Šema se može obrisati i ponovo napraviti jednom komandom tokom razvoja.

**Blocked by:** 01 — Repo skelet i Express server.

**Status:** done

- [x] Sve četiri tabele iz `db.md` sekcija 3 postoje sa tačnim tipovima kolona
- [x] Novac je `BIGINT` u minor jedinicama, kalendarski datumi su `DATE`, trenuci su `DATETIME(3)` u UTC
- [x] Svih sedam indeksa iz `db.md` postoji i vidljivi su kroz `SHOW INDEX`
- [x] Jedinstvenost email adrese je sprovedena na nivou baze
- [x] Jedinstvenost naziva lokacije važi po korisniku, ne globalno — dva korisnika mogu imati „Kuhinja"
- [x] Strani ključ ka kategoriji i lokaciji je `RESTRICT` i stvarno blokira brisanje entiteta u upotrebi
- [x] `CHECK` ograničenja odbijaju količinu manju od 1 i negativnu cenu
- [x] Jedanaest kategorija iz `db.md` sekcija 4.1 je ubačeno sa ispravnim redosledom i ključem ikonice
- [x] Komanda za kreiranje baze je idempotentna — dvostruko pokretanje ne prijavljuje grešku
- [x] Postoji komanda za potpuni reset baze, jasno označena kao razvojna

**Napomena:** Sve stavke su uživo provereno na lokalnoj MySQL 8.0 bazi (`MySQL80` servis): `npm run db:create` pokrenut dvaput bez greške (idempotentno), `SHOW INDEX` potvrđuje svih 9 indeksa iz `schema.sql` (uključujući i `idx_locations_user`, koji `db.md` deklariše u DDL-u ali ne pominje u tabeli objašnjenja), `uq_users_email` i `uq_locations_user_name` odbijaju duplikate a dozvoljavaju istu lokaciju kod dva različita korisnika, `chk_items_quantity`/`chk_items_prices` odbijaju `quantity=0` i negativnu cenu, `RESTRICT` blokira brisanje kategorije/lokacije u upotrebi. Baza je posle testa vraćena u čisto stanje sa `npm run db:reset` (11 kategorija, nula ostalih redova).
