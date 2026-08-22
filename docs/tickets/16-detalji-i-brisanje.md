# 16: Detalji predmeta i brisanje

**What to build:** Korisnik klikne na stvar iz liste i vidi sve što o njoj zna, na zasebnom ekranu. Odatle je može izmeniti ili ukloniti iz evidencije, uz pitanje da li je siguran. Ovaj ekran je i dokaz da se podaci ispravno prenose između aktivnosti.

**Blocked by:** 15 — Dodavanje i izmena predmeta.

**Status:** ready-for-agent

- [ ] Detalji se otvaraju u zasebnoj aktivnosti
- [ ] Aktivnost prima isključivo identifikator predmeta i sama učitava podatke iz lokalne baze (BR-007)
- [ ] Prikazuju se sva popunjena polja; prazna polja se ne prikazuju kao prazni redovi
- [ ] Fotografija se prikazuje ako postoji
- [ ] Ekran ima akciju za izmenu, koja otvara istu formu iz tiketa 15 u režimu izmene
- [ ] Ekran ima akciju za brisanje
- [ ] Brisanje traži potvrdu kroz dijalog u kojem se pominje naziv predmeta (BR-008)
- [ ] Brisanje označava predmet kao obrisan umesto da ga uklanja, i pokušava da to javi serveru
- [ ] Obrisan predmet odmah nestaje iz liste, pretrage i svih zbirova
- [ ] Posle brisanja se prikazuje kratka poruka sa mogućnošću opoziva u roku od pet sekundi (FR-027)
- [ ] Nepostojeći ili tuđ identifikator prikazuje poruku umesto rušenja aplikacije
- [ ] Povratak na listu prikazuje ažurirano stanje
