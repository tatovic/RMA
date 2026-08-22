# 14: Lista inventara

**What to build:** Korisnik otvara svoj inventar i vidi sve što poseduje, kao listu kroz koju može da skroluje. Svaka stavka mu na prvi pogled kaže šta je, gde stoji i koliko vredi. Ovo je ekran na kojem će korisnik provoditi najviše vremena.

**Blocked by:** 13 — Pregled stanja i zatvaranje vertikalnog preseka.

**Status:** ready-for-agent

- [ ] Lista prikazuje sve neobrisane predmete prijavljenog korisnika
- [ ] Svaka stavka prikazuje naziv, kategoriju, lokaciju i cenu
- [ ] Predmet bez fotografije prikazuje ikonicu svoje kategorije umesto prazne površine (FR-087)
- [ ] Lista se osvežava sama kada se podaci u lokalnoj bazi promene
- [ ] Prazan inventar prikazuje objašnjenje i poziv na dodavanje prvog predmeta
- [ ] Postoji dugme za dodavanje novog predmeta, uvek dostupno preko liste
- [ ] Skrolovanje je glatko sa pet stotina predmeta (NFR-01)
- [ ] Poređenje stavki pri osvežavanju se radi po identifikatoru, da lista ne treperi
- [ ] Klik na stavku otvara detalje, čak i ako je taj ekran u ovom trenutku još prazan
