# 05: CRUD predmeta sa proverom vlasništva

**What to build:** Srce sistema. Korisnik može preko API-ja da doda, pročita, izmeni i obriše predmet iz svog inventara — i ne može ni na koji način da dođe do tuđeg. Brisanje ne uništava podatak nego ga označava, a lista ume da vrati samo ono što se promenilo od zadatog trenutka, što je preduslov za kasniju sinhronizaciju.

**Blocked by:** 04 — Zaštita ruta i korisnički profil.

**Status:** ready-for-agent

- [ ] Kreiranje prihvata identifikator koji je generisao klijent i sva polja iz mapiranja u `db.md` sekcija 7
- [ ] Vlasnik se uvek uzima iz tokena; ako klijent pošalje vlasnika u telu zahteva, server ga ignoriše (OWN-02)
- [ ] Ponovno slanje istog identifikatora ne pravi duplikat nego vraća postojeći predmet
- [ ] Kategorija mora postojati, a lokacija mora pripadati istom korisniku, inače `404` (OWN-04)
- [ ] Čitanje, izmena i brisanje tuđeg predmeta vraćaju `404`, nikada `403` (OWN-03)
- [ ] Brisanje označava predmet kao obrisan umesto da uklanja red; ponovljeno brisanje je i dalje uspešno
- [ ] Obrisani predmeti se podrazumevano ne pojavljuju u listi
- [ ] Lista podržava parametar za preuzimanje samo izmena od zadatog trenutka i tada vraća i obrisane predmete, da bi klijent znao šta da ukloni
- [ ] Odgovor liste sadrži serversko vreme koje klijent čuva za sledeće preuzimanje izmena
- [ ] Izmena poredi vreme izmene koje je poslao klijent sa serverskim; starija verzija se odbija sa `409 SYNC_CONFLICT` i vraća serversku verziju (DB-RULE-04)
- [ ] Nijedan upit nad predmetima se ne izvršava bez uslova o vlasniku
- [ ] Svi upiti su parametrizovani; nigde nema sastavljanja SQL-a spajanjem stringova
