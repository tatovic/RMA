# 03: Registracija i prijava

**What to build:** Čovek može da napravi nalog i da se prijavi. Registracija vraća token odmah, bez dodatnog koraka prijave. Prvi nalog u sistemu postaje administrator. Svaki novi korisnik dobija svoj dom već podeljen na devet prostorija, pa može odmah da unosi predmete.

**Blocked by:** 02 — MySQL šema i globalne kategorije.

**Status:** ready-for-agent

- [ ] Registracija prihvata ime, email, lozinku i potvrdu lozinke i validira ih po VR-01 do VR-05
- [ ] Lozinka se hashuje BCrypt algoritmom sa cost faktorom 10; u bazi ne postoji čitljiva lozinka
- [ ] Registracija vraća `201` sa tokenom i podacima korisnika, bez polja sa hashom
- [ ] Postojeći email vraća `409 EMAIL_ALREADY_EXISTS`
- [ ] Prvi korisnik u sistemu dobija rolu ADMIN, svaki sledeći USER (BR-001)
- [ ] Registracija u istoj transakciji kreira korisnika i njegovih devet podrazumevanih lokacija (BR-015); ako bilo šta pukne, ne ostaje polovičan nalog
- [ ] Prijava vraća token sa rokom važenja od sedam dana
- [ ] Token nosi samo identifikator korisnika, rolu i vremena izdavanja i isteka — ništa osetljivo
- [ ] Nepostojeći email i pogrešna lozinka vraćaju istu poruku `INVALID_CREDENTIALS` (FR-017)
- [ ] Prijava deaktiviranog naloga vraća `403 ACCOUNT_DEACTIVATED`
- [ ] Tajna za potpisivanje tokena se čita iz konfiguracije i nije u kodu
