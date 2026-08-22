# 20: Filteri i sortiranje

**What to build:** Korisnik sužava listu na ono što ga zanima: samo elektronika iz dnevne sobe skuplja od pedeset hiljada, kupljena prošle godine, kojoj još traje garancija. Filteri i pretraga rade zajedno, a izabrani redosled se pamti za sledeći put.

**Blocked by:** 19 — Pretraga inventara.

**Status:** ready-for-agent

- [ ] Filteri se otvaraju u panelu sa dna ekrana, sa dugmeta pored pretrage
- [ ] Panel i lista dele isto stanje kroz zajednički ViewModel, pa se promena filtera odmah odražava na listu
- [ ] Filter po kategoriji dozvoljava više izbora
- [ ] Filter po lokaciji dozvoljava više izbora
- [ ] Filter po rasponu cene poredi vrednosti u valuti prikaza, ne sirove brojeve (BR-009)
- [ ] Filter po godini kupovine
- [ ] Filteri za predmete pod garancijom i za one kojima garancija uskoro ističe
- [ ] Broj aktivnih filtera je vidljiv na dugmetu
- [ ] Postoji dugme za poništavanje svih filtera odjednom
- [ ] Filteri i pretraga se kombinuju logičkim I
- [ ] Dostupno je svih šest načina sortiranja iz FR-038
- [ ] Izabrano sortiranje se pamti između pokretanja aplikacije
- [ ] Aktivni filteri i sortiranje preživljavaju rotaciju ekrana
- [ ] Kombinacija filtera bez rezultata prikazuje prazno stanje, ne praznu listu
