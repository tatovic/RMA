# 26: Puna sinhronizacija i offline rad

**What to build:** Korisnik u podrumu bez signala dodaje tri stvari, menja cenu četvrtoj i briše petu. Kad izađe napolje, sve to samo od sebe stigne na server, bez ijednog izgubljenog unosa. Ovo je najrizičniji deo projekta i zato ima sopstveni tiket.

**Blocked by:** 25 — Administratorski ekrani.

**Status:** ready-for-agent

- [ ] Slanje lokalnih izmena se uvek izvršava pre preuzimanja sa servera, inače bi serverska verzija pregazila lokalni rad
- [ ] Sva tri stanja čekanja se obrađuju: kreiranje, izmena i brisanje
- [ ] Predmet kreiran i obrisan pre nego što je server za njega saznao se briše lokalno, bez poziva servera (DB-RULE-03)
- [ ] Preuzimanje traži samo izmene od poslednjeg puta, ne ceo inventar (FR-098)
- [ ] Preuzimanje obrađuje i obrisane predmete, pa brisanje sa drugog uređaja stigne i ovde
- [ ] Lokalna izmena koja još nije poslata ima prednost nad serverskom verzijom dok se ne pošalje
- [ ] Pri prepisivanju reda sa servera se čuva lokalna putanja do fotografije (DB-RULE-02, FR-085)
- [ ] Konflikt se rešava po vremenu poslednje izmene; kad server odbije, njegova verzija prepisuje lokalnu (DB-RULE-04)
- [ ] Vreme poslednje sinhronizacije se uzima iz odgovora servera, nikada sa sata uređaja
- [ ] Sinhronizacija se pokreće pri otvaranju pregleda i inventara i posle svake izmene
- [ ] Korisnik može ručno da je pokrene povlačenjem liste nadole
- [ ] Stavke koje čekaju sinhronizaciju imaju vidljivu oznaku u listi (FR-096)
- [ ] Neuspela sinhronizacija ne blokira rad i ne prazni ekran (FR-097)
- [ ] Odgovor 401 briše sesiju i lokalnu bazu i vraća korisnika na prijavu (FR-009)
- [ ] Provereno na uređaju: rad u avionskom režimu, pa uključivanje mreže, pa provera da su svi podaci stigli u MySQL
