# 04: Zaštita ruta i korisnički profil

**What to build:** Zaštićeni delovi API-ja prepoznaju ko šalje zahtev i odbijaju svakoga bez važećeg tokena. Prijavljen korisnik može da pročita i izmeni svoj profil i da promeni lozinku. Deaktivacija naloga počinje da deluje odmah, a ne tek kad token istekne.

**Blocked by:** 03 — Registracija i prijava.

**Status:** ready-for-agent

- [ ] Zahtev bez tokena ka zaštićenoj ruti vraća `401 TOKEN_INVALID`
- [ ] Istekao token vraća `401 TOKEN_EXPIRED`
- [ ] Middleware učitava korisnika iz baze pri svakom zahtevu i proverava da li je nalog aktivan — nije dovoljno verovati sadržaju tokena (FR-014)
- [ ] Deaktiviran korisnik sa još uvek važećim tokenom dobija `403 ACCOUNT_DEACTIVATED` na svaki zahtev
- [ ] Identifikator korisnika je dostupan narednim slojevima i uvek potiče iz verifikovanog tokena (OWN-01)
- [ ] Čitanje profila vraća ime, email, rolu, status i valutu prikaza
- [ ] Izmena profila prihvata ime i valutu prikaza, oba opciono; valuta mora biti iz liste od šest podržanih (BR-012)
- [ ] Promena lozinke traži tačnu trenutnu lozinku, inače `400 WRONG_CURRENT_PASSWORD`
- [ ] Nova lozinka prolazi istu validaciju kao pri registraciji i čuva se hashovana
