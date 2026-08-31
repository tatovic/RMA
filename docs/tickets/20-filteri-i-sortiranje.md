# 20: Filteri i sortiranje

**What to build:** Korisnik sužava listu na ono što ga zanima: samo elektronika iz dnevne sobe skuplja od pedeset hiljada, kupljena prošle godine, kojoj još traje garancija. Filteri i pretraga rade zajedno, a izabrani redosled se pamti za sledeći put.

**Blocked by:** 19 — Pretraga inventara.

**Status:** done

- [x] Filteri se otvaraju u panelu sa dna ekrana, sa dugmeta pored pretrage
- [x] Panel i lista dele isto stanje kroz zajednički ViewModel, pa se promena filtera odmah odražava na listu
- [x] Filter po kategoriji dozvoljava više izbora
- [x] Filter po lokaciji dozvoljava više izbora
- [x] Filter po rasponu cene poredi vrednosti u valuti prikaza, ne sirove brojeve (BR-009)
- [x] Filter po godini kupovine
- [x] Filteri za predmete pod garancijom i za one kojima garancija uskoro ističe
- [x] Broj aktivnih filtera je vidljiv na dugmetu
- [x] Postoji dugme za poništavanje svih filtera odjednom
- [x] Filteri i pretraga se kombinuju logičkim I
- [x] Dostupno je svih šest načina sortiranja iz FR-038
- [x] Izabrano sortiranje se pamti između pokretanja aplikacije
- [x] Aktivni filteri i sortiranje preživljavaju rotaciju ekrana
- [x] Kombinacija filtera bez rezultata prikazuje prazno stanje, ne praznu listu

Napomena: konverzija valuta (kursna lista, BR-009 pun kurs) nije deo ovog tiketa — dok tiket 21 ne uvede
kursnu listu, filter po rasponu cene i sortiranje po ceni porede samo predmete čija se valuta poklapa
sa valutom prikaza korisnika; predmeti u drugoj valuti se izostavljaju iz filtera po ceni, odnosno
grupišu na kraj liste pri sortiranju po ceni — isti duh kao BR-013/tiket 13. Prag „garancija uskoro
ističe" koristi podrazumevanih 30 dana (FR-051 podesivi prag dolazi u tiketu 22).

Napomena: implementacija je gotova i projekat se uspešno builduje (`./gradlew :app:assembleDebug`) i
prolazi postojeće unit testove (`./gradlew :app:testDebugUnitTest`), ali nije ručno provereno na
emulatoru/uređaju — nema pristupa Android emulatoru u ovoj sesiji.
