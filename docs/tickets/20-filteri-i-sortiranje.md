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

Provereno ručno na emulatoru (Pixel6_API36) 2026-08-31: panel se otvara sa dugmeta pored pretrage;
biranje kategorije (Kuhinja) i godine kupovine (2024) filtrira listu na tačna tri predmeta i dugme
pokazuje značku „2"; kombinacija kategorije+lokacije+cene+garancije bez pogotka prikazuje poruku
„Nijedan predmet ne odgovara izabranim filterima" umesto prazne liste; „Poništi sve filtere" vraća
punu listu i uklanja značku; svih šest opcija sortiranja provereno (Najnovije/Najstarije dodato, Naziv
A-Z, Cena rastuće/opadajuće — potvrđeno da se stavke van valute prikaza grupišu na kraj liste po BR-009);
izabrano sortiranje preživelo prisilno gašenje i ponovno pokretanje aplikacije (DataStore); aktivni
filteri, sortiranje i značka preživeli rotaciju ekrana (portret→pejzaž→portret); pretraga i filteri
zajedno rade logičkim I (upit bez pogotka u filtriranoj kategoriji ispravno prikazuje poruku pretrage).

Napomena (van obima ovog tiketa, prijavljeno korisniku): tokom provere je otkriveno da polje za
pretragu (uvedeno tiketom 19) i novo dugme za filtere imaju gornji deo dodirne površine ispod visine
statusne trake/isečka kamere na Pixel 6-klasi uređaja (`layoutSearch`/`buttonFilter` počinju na svega
12dp od vrha ekrana, bez rukovanja WindowInsets-ima) — dodir tačno na sredini polja/dugmeta se ne
registruje, dok dodir u donjoj trećini radi. Ovo pogađa svaki ekran u aplikaciji podjednako (nijedan
fragment ne primenjuje `WindowInsets` padding), pa je popravka namerno ostavljena za poseban tiket
umesto tihog proširivanja obima ovog tiketa.
