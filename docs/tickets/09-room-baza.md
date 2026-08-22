# 09: Lokalna Room baza

**What to build:** Aplikacija dobija sopstvenu bazu na uređaju, mesto koje će biti jedini izvor istine za sve što se prikazuje na ekranu. Upiti za listu, pretragu, garancije i statistiku već postoje i pokriveni su testovima, iako ih još niko ne prikazuje.

**Blocked by:** 08 — Android projekat i build konfiguracija.

**Status:** ready-for-agent

- [ ] Svih šest entiteta iz db.md sekcija 5.1 postoji sa tačnim tipovima
- [ ] Kalendarski datumi se čuvaju kao tekst u formatu godina-mesec-dan, trenuci kao ceo broj milisekundi, novac kao ceo broj minor jedinica
- [ ] Entitet predmeta ima i dva polja kojih na serveru nema: putanju do fotografije i status sinhronizacije
- [ ] Indeksi iz db.md sekcija 5.1 su postavljeni
- [ ] Upit za listu spaja predmet sa kategorijom i lokacijom i vraća projekciju spremnu za prikaz
- [ ] Upit za pretragu pokriva svih šest polja iz FR-031
- [ ] Upit za garancije vraća predmete kojima datum ističe u zadatom rasponu, sortirane po hitnosti
- [ ] Upit za agregaciju grupiše po kategoriji i po valuti, jer se iznosi u različitim valutama ne smeju sabirati u SQL-u (BR-009)
- [ ] Svi upiti za prikaz izostavljaju obrisane predmete
- [ ] Postoji upit koji vraća sve predmete koji čekaju sinhronizaciju
- [ ] Postoji operacija koja briše kompletan sadržaj baze, za potrebe odjave (BR-005)
- [ ] Šema se izvozi u JSON i taj folder je commitovan u repozitorijum
- [ ] Instrumentirani testovi nad bazom u memoriji potvrđuju pretragu, agregaciju i izostavljanje obrisanih predmeta
