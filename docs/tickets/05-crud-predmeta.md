# 05: CRUD predmeta sa proverom vlasništva

**What to build:** Srce sistema. Korisnik može preko API-ja da doda, pročita, izmeni i obriše predmet iz svog inventara — i ne može ni na koji način da dođe do tuđeg. Brisanje ne uništava podatak nego ga označava, a lista ume da vrati samo ono što se promenilo od zadatog trenutka, što je preduslov za kasniju sinhronizaciju.

**Blocked by:** 04 — Zaštita ruta i korisnički profil.

**Status:** done

- [x] Kreiranje prihvata identifikator koji je generisao klijent i sva polja iz mapiranja u `db.md` sekcija 7
- [x] Vlasnik se uvek uzima iz tokena; ako klijent pošalje vlasnika u telu zahteva, server ga ignoriše (OWN-02)
- [x] Ponovno slanje istog identifikatora ne pravi duplikat nego vraća postojeći predmet
- [x] Kategorija mora postojati, a lokacija mora pripadati istom korisniku, inače `404` (OWN-04)
- [x] Čitanje, izmena i brisanje tuđeg predmeta vraćaju `404`, nikada `403` (OWN-03)
- [x] Brisanje označava predmet kao obrisan umesto da uklanja red; ponovljeno brisanje je i dalje uspešno
- [x] Obrisani predmeti se podrazumevano ne pojavljuju u listi
- [x] Lista podržava parametar za preuzimanje samo izmena od zadatog trenutka i tada vraća i obrisane predmete, da bi klijent znao šta da ukloni
- [x] Odgovor liste sadrži serversko vreme koje klijent čuva za sledeće preuzimanje izmena
- [x] Izmena poredi vreme izmene koje je poslao klijent sa serverskim; starija verzija se odbija sa `409 SYNC_CONFLICT` i vraća serversku verziju (DB-RULE-04)
- [x] Nijedan upit nad predmetima se ne izvršava bez uslova o vlasniku
- [x] Svi upiti su parametrizovani; nigde nema sastavljanja SQL-a spajanjem stringova
