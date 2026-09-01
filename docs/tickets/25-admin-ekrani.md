# 25: Administratorski ekrani

**What to build:** Administrator ulazi u zaseban deo aplikacije iz svog profila. Tamo vidi stanje sistema, upravlja spiskom kategorija koji svi korisnici dele, i može da isključi nalog. Obični korisnik ni ne zna da taj deo postoji.

**Blocked by:** 24 — Administratorski deo API-ja.

**Status:** done

- [x] Ulaz u administraciju je vidljiv u profilu samo korisniku sa administratorskom rolom (FR-100)
- [x] Administracija se otvara kao zasebna aktivnost sa sopstvenom navigacijom i tri ekrana
- [x] Pregled sistema prikazuje broj korisnika po statusu, ukupan broj predmeta i broj kategorija
- [x] Ekran korisnika prikazuje ime, email, rolu, status i broj predmeta
- [x] Prekidač menja status naloga, uz dijalog potvrde (BR-008)
- [x] Pokušaj deaktivacije sopstvenog naloga prikazuje razumljivu poruku
- [x] Ekran kategorija omogućava dodavanje, preimenovanje i brisanje globalnih kategorija
- [x] Uz svaku kategoriju stoji broj predmeta koji je koriste
- [x] Brisanje kategorije u upotrebi je odbijeno, uz poruku sa brojem predmeta
- [x] Duplikat naziva kategorije daje razumljivu poruku
- [x] Nijedan administratorski ekran ne prikazuje sadržaj tuđeg inventara (BR-002)
- [x] Deaktiviran korisnik zaista ne može da se prijavi, provereno na uređaju
