# 03: Registracija i prijava

**What to build:** Čovek može da napravi nalog i da se prijavi. Registracija vraća token odmah, bez dodatnog koraka prijave. Prvi nalog u sistemu postaje administrator. Svaki novi korisnik dobija svoj dom već podeljen na devet prostorija, pa može odmah da unosi predmete.

**Blocked by:** 02 — MySQL šema i globalne kategorije.

**Status:** ready-for-agent

- [x] Registracija prihvata ime, email, lozinku i potvrdu lozinke i validira ih po VR-01 do VR-05
- [x] Lozinka se hashuje BCrypt algoritmom sa cost faktorom 10; u bazi ne postoji čitljiva lozinka
- [ ] Registracija vraća `201` sa tokenom i podacima korisnika, bez polja sa hashom
- [ ] Postojeći email vraća `409 EMAIL_ALREADY_EXISTS`
- [ ] Prvi korisnik u sistemu dobija rolu ADMIN, svaki sledeći USER (BR-001)
- [ ] Registracija u istoj transakciji kreira korisnika i njegovih devet podrazumevanih lokacija (BR-015); ako bilo šta pukne, ne ostaje polovičan nalog
- [ ] Prijava vraća token sa rokom važenja od sedam dana
- [x] Token nosi samo identifikator korisnika, rolu i vremena izdavanja i isteka — ništa osetljivo
- [ ] Nepostojeći email i pogrešna lozinka vraćaju istu poruku `INVALID_CREDENTIALS` (FR-017)
- [ ] Prijava deaktiviranog naloga vraća `403 ACCOUNT_DEACTIVATED`
- [x] Tajna za potpisivanje tokena se čita iz konfiguracije i nije u kodu

**Napomena:** Implementirano — `backend/src/modules/auth/` (`.schema` sa zod za VR-01..VR-05, `.service` sa transakcijom registracije po BR-001/BR-015, `.controller`, `.routes`), `backend/src/middleware/validate.js`, `backend/src/utils/serializer.js`, `backend/src/db/locations.js` (devet lokacija), montirano na `/api/auth` u `app.js`. Validacija (VR-01..VR-05), BCrypt hashovanje (cost 10, potvrđeno da hash string sadrži `$10$` i da je dužine 60) i JWT mehanizam (payload sadrži samo `sub`/`role`/`iat`/`exp`, tajna iz `env.JWT_SECRET`, rok 7 dana) su provereni izolovano bez baze. Stavke koje zahtevaju **stvarno izvršavanje** nad lokalnom MySQL bazom (201 odgovor sa upisanim korisnikom, `409 EMAIL_ALREADY_EXISTS`, dodela ADMIN/USER role po BR-001, atomičnost transakcije sa devet lokacija po BR-015, `INVALID_CREDENTIALS` za oba slučaja po FR-017, `403 ACCOUNT_DEACTIVATED`) nisu mogle biti provere­ne u ovoj sesiji — isti uzrok kao u tiketu 02: lokalna MySQL root lozinka nije poznata. Korisnik je odlučio da se nastavi bez uživo testa. Kad tačna lozinka bude poznata, pokrenuti `npm run db:reset` pa registraciju/prijavu preko Postman-a ili curl-a i doštiklirati preostale stavke.
