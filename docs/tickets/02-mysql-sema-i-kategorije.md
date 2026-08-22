# 02: MySQL šema i globalne kategorije

**What to build:** Baza podataka postoji i sprovodi pravila sama od sebe. Tabele, relacije, indeksi i ograničenja su na mestu, a sistem ima svojih jedanaest globalnih kategorija koje će svi korisnici deliti. Šema se može obrisati i ponovo napraviti jednom komandom tokom razvoja.

**Blocked by:** 01 — Repo skelet i Express server.

**Status:** ready-for-agent

- [ ] Sve četiri tabele iz `db.md` sekcija 3 postoje sa tačnim tipovima kolona
- [ ] Novac je `BIGINT` u minor jedinicama, kalendarski datumi su `DATE`, trenuci su `DATETIME(3)` u UTC
- [ ] Svih sedam indeksa iz `db.md` postoji i vidljivi su kroz `SHOW INDEX`
- [ ] Jedinstvenost email adrese je sprovedena na nivou baze
- [ ] Jedinstvenost naziva lokacije važi po korisniku, ne globalno — dva korisnika mogu imati „Kuhinja"
- [ ] Strani ključ ka kategoriji i lokaciji je `RESTRICT` i stvarno blokira brisanje entiteta u upotrebi
- [ ] `CHECK` ograničenja odbijaju količinu manju od 1 i negativnu cenu
- [ ] Jedanaest kategorija iz `db.md` sekcija 4.1 je ubačeno sa ispravnim redosledom i ključem ikonice
- [ ] Komanda za kreiranje baze je idempotentna — dvostruko pokretanje ne prijavljuje grešku
- [ ] Postoji komanda za potpuni reset baze, jasno označena kao razvojna
