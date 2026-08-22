# 25: Administratorski ekrani

**What to build:** Administrator ulazi u zaseban deo aplikacije iz svog profila. Tamo vidi stanje sistema, upravlja spiskom kategorija koji svi korisnici dele, i može da isključi nalog. Obični korisnik ni ne zna da taj deo postoji.

**Blocked by:** 24 — Administratorski deo API-ja.

**Status:** ready-for-agent

- [ ] Ulaz u administraciju je vidljiv u profilu samo korisniku sa administratorskom rolom (FR-100)
- [ ] Administracija se otvara kao zasebna aktivnost sa sopstvenom navigacijom i tri ekrana
- [ ] Pregled sistema prikazuje broj korisnika po statusu, ukupan broj predmeta i broj kategorija
- [ ] Ekran korisnika prikazuje ime, email, rolu, status i broj predmeta
- [ ] Prekidač menja status naloga, uz dijalog potvrde (BR-008)
- [ ] Pokušaj deaktivacije sopstvenog naloga prikazuje razumljivu poruku
- [ ] Ekran kategorija omogućava dodavanje, preimenovanje i brisanje globalnih kategorija
- [ ] Uz svaku kategoriju stoji broj predmeta koji je koriste
- [ ] Brisanje kategorije u upotrebi je odbijeno, uz poruku sa brojem predmeta
- [ ] Duplikat naziva kategorije daje razumljivu poruku
- [ ] Nijedan administratorski ekran ne prikazuje sadržaj tuđeg inventara (BR-002)
- [ ] Deaktiviran korisnik zaista ne može da se prijavi, provereno na uređaju
