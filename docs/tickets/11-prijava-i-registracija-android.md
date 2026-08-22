# 11: Prijava i registracija na uređaju

**What to build:** Čovek uzme telefon, napravi nalog i uđe u aplikaciju. Sledeći put kad je otvori, već je unutra, sve dok mu sesija ne istekne. Ovo je prvi tiket u kojem podatak putuje celim putem: sa ekrana, kroz mrežu, do MySQL baze i nazad.

**Blocked by:** 10 — Mrežni sloj i obrada grešaka.

**Status:** ready-for-agent

- [ ] Postoji ulazna aktivnost sa dva ekrana, za prijavu i za registraciju, i prelaz između njih
- [ ] Registracija traži ime, email, lozinku i potvrdu lozinke; greške se prikazuju ispod odgovarajućeg polja na srpskom (VR-01 do VR-05)
- [ ] Uspešna registracija odmah prijavljuje korisnika, bez dodatnog koraka (FR-018)
- [ ] Tokom slanja zahteva prikazan je indikator, a dugme je onemogućeno da se ne pošalje dvaput
- [ ] Zauzet email, pogrešni kredencijali i deaktiviran nalog daju razumljive poruke iz kataloga grešaka
- [ ] Bez interneta se prikazuje poruka o nedostatku konekcije, a aplikacija se ne ruši
- [ ] Token, identifikator korisnika i rola se čuvaju u trajnom lokalnom skladištu; lozinka se ne čuva nigde
- [ ] Pri pokretanju aplikacije proverava se važenje sesije i bira se početni ekran (FR-011)
- [ ] Posle uspešne prijave povratno dugme ne vraća na ekran prijave
- [ ] Nijedan tekst na ova dva ekrana nije hardkodovan (NFR-09)
