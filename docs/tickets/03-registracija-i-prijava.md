# 03: Registracija i prijava

**What to build:** Čovek može da napravi nalog i da se prijavi. Registracija vraća token odmah, bez dodatnog koraka prijave. Prvi nalog u sistemu postaje administrator. Svaki novi korisnik dobija svoj dom već podeljen na devet prostorija, pa može odmah da unosi predmete.

**Blocked by:** 02 — MySQL šema i globalne kategorije.

**Status:** done

- [x] Registracija prihvata ime, email, lozinku i potvrdu lozinke i validira ih po VR-01 do VR-05
- [x] Lozinka se hashuje BCrypt algoritmom sa cost faktorom 10; u bazi ne postoji čitljiva lozinka
- [x] Registracija vraća `201` sa tokenom i podacima korisnika, bez polja sa hashom
- [x] Postojeći email vraća `409 EMAIL_ALREADY_EXISTS`
- [x] Prvi korisnik u sistemu dobija rolu ADMIN, svaki sledeći USER (BR-001)
- [x] Registracija u istoj transakciji kreira korisnika i njegovih devet podrazumevanih lokacija (BR-015); ako bilo šta pukne, ne ostaje polovičan nalog
- [x] Prijava vraća token sa rokom važenja od sedam dana
- [x] Token nosi samo identifikator korisnika, rolu i vremena izdavanja i isteka — ništa osetljivo
- [x] Nepostojeći email i pogrešna lozinka vraćaju istu poruku `INVALID_CREDENTIALS` (FR-017)
- [x] Prijava deaktiviranog naloga vraća `403 ACCOUNT_DEACTIVATED`
- [x] Tajna za potpisivanje tokena se čita iz konfiguracije i nije u kodu

**Napomena:** Sve stavke su uživo provereno na lokalnoj MySQL bazi (posle promene root lozinke) preko `npm run dev` + `curl`. Registracija prvog korisnika (`marko@primer.rs`) vratila je `201` sa rolom `ADMIN` i tokenom bez `password_hash` polja; drugi korisnik (`ana@primer.rs`) dobio je rolu `USER` (BR-001). Direktan upit nad bazom potvrđuje `password_hash` dužine 60 sa `$2b$10$` prefiksom (BCrypt cost 10) i tačno devet redova u `locations` po korisniku sa imenima iz `db.md` 4.2 (BR-015). Ponovna registracija sa istim email-om vratila je `409 EMAIL_ALREADY_EXISTS`; namerno neispravan zahtev vratio je `400 VALIDATION_ERROR` sa `details` po polju. Prijava sa ispravnim kredencijalima vratila je `200` sa istim tokenom (payload dekodiran: samo `sub`/`role`/`iat`/`exp`, razlika `exp - iat` = 7 dana); prijava sa nepostojećim email-om i sa pogrešnom lozinkom vratile su identičnu `401 INVALID_CREDENTIALS` poruku (FR-017). Ručno deaktiviran nalog (`is_active = 0`) vratio je `403 ACCOUNT_DEACTIVATED` pri prijavi. Baza je posle testa vraćena u čisto stanje sa `npm run db:reset`.
