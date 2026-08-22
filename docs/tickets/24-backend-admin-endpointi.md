# 24: Administratorski deo API-ja

**What to build:** Administrator dobija pravo da vidi ko je sve u sistemu i da isključi nalog koji pravi probleme. Ono što ne dobija je pristup tuđim stvarima: server mu daje brojeve, ne sadržaj. Povratak na Node posle dužeg rada na Androidu je namerni deo plana.

**Blocked by:** 23 — Statistika sa grafikonima.

**Status:** ready-for-agent

- [ ] Postoji provera role koja štiti sve administratorske rute; korisnik dobija 403 (FR-106)
- [ ] Sistemska statistika vraća broj registrovanih, aktivnih i deaktiviranih korisnika, ukupan broj predmeta i broj kategorija
- [ ] Lista korisnika vraća ime, email, rolu, status, datum registracije i broj predmeta
- [ ] Lista korisnika ne izlaže nijedan podatak o sadržaju tuđeg inventara (OWN-06)
- [ ] Administrator može promeniti status naloga na aktivan ili neaktivan
- [ ] Pokušaj deaktivacije sopstvenog naloga vraća 409 CANNOT_DEACTIVATE_SELF (BR-004)
- [ ] Deaktiviran korisnik odmah gubi pristup, i ako mu je token još važeći
- [ ] Postman kolekcija je dopunjena administratorskim scenarijima, uključujući proveru da korisnik dobija 403
