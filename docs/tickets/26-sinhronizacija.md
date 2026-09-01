# 26: Puna sinhronizacija i offline rad

**What to build:** Korisnik u podrumu bez signala dodaje tri stvari, menja cenu četvrtoj i briše petu. Kad izađe napolje, sve to samo od sebe stigne na server, bez ijednog izgubljenog unosa. Ovo je najrizičniji deo projekta i zato ima sopstveni tiket.

**Blocked by:** 25 — Administratorski ekrani.

**Status:** done

- [x] Slanje lokalnih izmena se uvek izvršava pre preuzimanja sa servera, inače bi serverska verzija pregazila lokalni rad
- [x] Sva tri stanja čekanja se obrađuju: kreiranje, izmena i brisanje
- [x] Predmet kreiran i obrisan pre nego što je server za njega saznao se briše lokalno, bez poziva servera (DB-RULE-03)
- [x] Preuzimanje traži samo izmene od poslednjeg puta, ne ceo inventar (FR-098)
- [x] Preuzimanje obrađuje i obrisane predmete, pa brisanje sa drugog uređaja stigne i ovde
- [x] Lokalna izmena koja još nije poslata ima prednost nad serverskom verzijom dok se ne pošalje
- [x] Pri prepisivanju reda sa servera se čuva lokalna putanja do fotografije (DB-RULE-02, FR-085)
- [x] Konflikt se rešava po vremenu poslednje izmene; kad server odbije, njegova verzija prepisuje lokalnu (DB-RULE-04)
- [x] Vreme poslednje sinhronizacije se uzima iz odgovora servera, nikada sa sata uređaja
- [x] Sinhronizacija se pokreće pri otvaranju pregleda i inventara i posle svake izmene
- [x] Korisnik može ručno da je pokrene povlačenjem liste nadole
- [x] Stavke koje čekaju sinhronizaciju imaju vidljivu oznaku u listi (FR-096)
- [x] Neuspela sinhronizacija ne blokira rad i ne prazni ekran (FR-097)
- [x] Odgovor 401 briše sesiju i lokalnu bazu i vraća korisnika na prijavu (FR-009)
- [x] Provereno na uređaju: rad u avionskom režimu, pa uključivanje mreže, pa provera da su svi podaci stigli u MySQL
