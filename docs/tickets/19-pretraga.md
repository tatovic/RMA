# 19: Pretraga inventara

**What to build:** Korisnik sa šezdeset ili petsto stvari kuca dve reči i nalazi tačno ono što traži. Pretraga gleda i naziv i proizvođača i model i serijski broj, ali i kategoriju i prostoriju, pa upit tipa garaža vraća sve iz garaže. Radi bez interneta.

**Blocked by:** 18 — Fotografije predmeta.

**Status:** done

- [x] Polje za pretragu je vidljivo direktno na ekranu inventara, bez dodatnog otvaranja
- [x] Pretražuje se po svih šest polja iz FR-031: naziv, proizvođač, model, serijski broj, kategorija i lokacija
- [x] Pretraga ne razlikuje velika i mala slova
- [x] Pretraga radi i sa srpskim dijakritikama, pa upit sporet nalazi Šporet
- [x] Rezultati se osvežavaju dok korisnik kuca, sa zadrškom od 300 milisekundi (FR-032)
- [x] Upit se normalizuje pre slanja u bazu, a ne oslanja se samo na SQL poređenje
- [x] Prazan rezultat prikazuje objašnjenje sa pojmom koji je tražen
- [x] Postoji način da se pretraga brzo obriše i vrati puna lista
- [x] Pretraga radi bez interneta, jer se izvršava nad lokalnom bazom
- [x] Uneti pojam preživljava rotaciju ekrana (NFR-04)
