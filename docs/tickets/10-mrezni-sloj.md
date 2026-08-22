# 10: Mrežni sloj i obrada grešaka

**What to build:** Aplikacija ume da razgovara sa dva različita servera: sa našim backendom uz token, i sa spoljnim servisom za kurseve bez tokena. Svaka greška, od nestanka mreže do odgovora servera, pretvara se u jedan predvidiv oblik koji viši slojevi znaju da prikažu. Ništa se još ne vidi na ekranu, ali sve je pokriveno testovima.

**Blocked by:** 09 — Lokalna Room baza.

**Status:** ready-for-agent

- [ ] Postoje dva odvojena HTTP klijenta; token se šalje isključivo našem backendu, nikada trećoj strani
- [ ] Token se automatski dodaje u zaglavlje svakog zahteva ka backendu
- [ ] Definisani su svi endpointi iz tech.md sekcija 6, kao suspend funkcije
- [ ] Prenosne klase odgovaraju stvarnom obliku odgovora servera, uključujući razliku u imenovanju polja između JSON-a i Kotlina
- [ ] Postoji mapiranje između prenosnih klasa, entiteta baze i domenskih modela, kao tri odvojene vrste klasa
- [ ] Mapiranje sa servera u bazu prima i čuva postojeću lokalnu putanju do fotografije, koja na serveru ne postoji (DB-RULE-02)
- [ ] Jedna zajednička funkcija obrađuje svaki mrežni poziv i pretvara ishod u uspeh, grešku ili stanje učitavanja
- [ ] Nestanak mreže, istek veze i nedostupan server se razlikuju i daju različite poruke
- [ ] Telo greške sa servera se čita i njegov kod se prevodi u poruku iz kataloga u prd.md sekcija 10
- [ ] Tekst izuzetka se nikada ne prosleđuje do korisnika, ali se uvek loguje (ERR-03, ERR-04)
- [ ] Postoje klase stanja za rezultat operacije i za stanje ekrana, sa četiri stanja iz BR-017
- [ ] Detaljno logovanje mrežnog saobraćaja je uključeno samo u debug build-u
- [ ] Unit testovi pokrivaju mapiranje u oba smera i sve grane obrade grešaka
