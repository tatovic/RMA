# 27: Poliranje i provera projektnih zahteva

**What to build:** Aplikacija prestaje da bude zbir funkcionalnosti i postaje proizvod. Nijedan ekran ne pokazuje praznu belinu, nijedna greška ne pokazuje tekst izuzetka, i svaki projektni zahtev ima dokaz da je ispunjen. Ovo je tiket posle kojeg se projekat predaje.

**Blocked by:** 26 — Puna sinhronizacija i offline rad.

**Status:** ready-for-agent

- [ ] Svaki ekran ima sva četiri stanja iz BR-017: učitavanje, sadržaj, prazno i greška
- [ ] Svako prazno stanje ima ikonicu, objašnjenje i poziv na akciju
- [ ] Svako stanje greške ima razumljivu poruku i dugme za ponovni pokušaj
- [ ] Nijedna poruka korisniku ne sadrži tekst izuzetka ni stack trace (ERR-03)
- [ ] Sve poruke o greškama su prevedene po katalogu iz prd.md sekcija 10
- [ ] Indikator učitavanja postoji na svim operacijama iz prd.md sekcija 11, uz onemogućeno dugme
- [ ] Nijedan tekst vidljiv korisniku nije hardkodovan; sve je u resursima (NFR-09)
- [ ] Razarajuća migracija lokalne baze je uklonjena i napisana je prava migracija sa testom
- [ ] Tamna tema je proverena na svakom ekranu, uključujući grafikone
- [ ] Rotacija ekrana je proverena na svakom ekranu i ne gubi stanje (NFR-04)
- [ ] Aplikacija je testirana sa pet stotina predmeta i lista ostaje glatka (NFR-01)
- [ ] Detaljno logovanje mreže je isključeno u release build-u
- [ ] Provereno da nijedan upit na serveru ne radi bez uslova o vlasniku
- [ ] Svih petnaest stavki acceptance checkliste iz prd.md sekcija 14 je označeno i ima dokaz
- [ ] README opisuje kako se ceo sistem pokreće od nule, uključujući demo kredencijale
