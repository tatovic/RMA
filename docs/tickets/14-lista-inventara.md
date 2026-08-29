# 14: Lista inventara

**What to build:** Korisnik otvara svoj inventar i vidi sve što poseduje, kao listu kroz koju može da skroluje. Svaka stavka mu na prvi pogled kaže šta je, gde stoji i koliko vredi. Ovo je ekran na kojem će korisnik provoditi najviše vremena.

**Blocked by:** 13 — Pregled stanja i zatvaranje vertikalnog preseka.

**Status:** done

- [x] Lista prikazuje sve neobrisane predmete prijavljenog korisnika
- [x] Svaka stavka prikazuje naziv, kategoriju, lokaciju i cenu
- [x] Predmet bez fotografije prikazuje ikonicu svoje kategorije umesto prazne površine (FR-087)
- [x] Lista se osvežava sama kada se podaci u lokalnoj bazi promene
- [x] Prazan inventar prikazuje objašnjenje i poziv na dodavanje prvog predmeta
- [x] Postoji dugme za dodavanje novog predmeta, uvek dostupno preko liste
- [x] Skrolovanje je glatko sa pet stotina predmeta (NFR-01)
- [x] Poređenje stavki pri osvežavanju se radi po identifikatoru, da lista ne treperi
- [x] Klik na stavku otvara detalje, čak i ako je taj ekran u ovom trenutku još prazan

Provereno ručno na emulatoru (Pixel6_API36) 2026-08-29: prijava demo nalogom prikazuje svih 63 predmeta u listi (RecyclerView + ListAdapter), svaka stavka prikazuje naziv, kategoriju • lokaciju i cenu formatiranu preko MoneyFormatter-a; predmeti bez fotografije prikazuju ikonicu svoje kategorije (11 novih drawable-a po iconKey iz seed-a), skrolovanje kroz svih 63 predmeta je glatko; klik na stavku otvara ItemDetailsActivity sa EXTRA_ITEM_ID (ekran je i dalje placeholder do tiketa 16); FAB i dalje dostupan preko liste; nov nalog bez predmeta prikazuje prazno stanje sa CTA "Dodaj predmet" koje vodi na SCR-06; pull-to-refresh ne remeti redosled niti treperi listu (DiffUtil po id-u); gašenje mreže (adb) uz postojeće lokalne podatke prikazuje kratku Snackbar poruku ("Server trenutno nije dostupan." + "Pokušaj ponovo"), ne ceo ekran greške (ERR-05).
