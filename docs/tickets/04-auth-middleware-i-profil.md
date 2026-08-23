# 04: Zaštita ruta i korisnički profil

**What to build:** Zaštićeni delovi API-ja prepoznaju ko šalje zahtev i odbijaju svakoga bez važećeg tokena. Prijavljen korisnik može da pročita i izmeni svoj profil i da promeni lozinku. Deaktivacija naloga počinje da deluje odmah, a ne tek kad token istekne.

**Blocked by:** 03 — Registracija i prijava.

**Status:** done

- [x] Zahtev bez tokena ka zaštićenoj ruti vraća `401 TOKEN_INVALID`
- [x] Istekao token vraća `401 TOKEN_EXPIRED`
- [x] Middleware učitava korisnika iz baze pri svakom zahtevu i proverava da li je nalog aktivan — nije dovoljno verovati sadržaju tokena (FR-014)
- [x] Deaktiviran korisnik sa još uvek važećim tokenom dobija `403 ACCOUNT_DEACTIVATED` na svaki zahtev
- [x] Identifikator korisnika je dostupan narednim slojevima i uvek potiče iz verifikovanog tokena (OWN-01)
- [x] Čitanje profila vraća ime, email, rolu, status i valutu prikaza
- [x] Izmena profila prihvata ime i valutu prikaza, oba opciono; valuta mora biti iz liste od šest podržanih (BR-012)
- [x] Promena lozinke traži tačnu trenutnu lozinku, inače `400 WRONG_CURRENT_PASSWORD`
- [x] Nova lozinka prolazi istu validaciju kao pri registraciji i čuva se hashovana

**Napomena:** Sve stavke su uživo provereno na lokalnoj MySQL bazi (posle promene root lozinke) preko `node src/server.js` + `curl`. Zahtev bez tokena i sa istekim tokenom vratili su `401 TOKEN_INVALID`/`401 TOKEN_EXPIRED` pre bilo kakvog upita nad bazom. `GET /api/users/me` sa validnim tokenom vratio je ime, email, rolu, status i valutu; `PATCH /api/users/me` sa `{"name":..., "currency":"EUR"}` je izmenio oba polja i naredni `GET` je potvrdio promenu, dok je `{"currency":"JPY"}` vratio `400 VALIDATION_ERROR` s porukom „Nepodržana valuta" (BR-012). `POST /api/users/me/password` sa pogrešnom trenutnom lozinkom vratio je `400 WRONG_CURRENT_PASSWORD`; sa tačnom je vratio `204`, posle čega je prijava sa starom lozinkom pala na `401 INVALID_CREDENTIALS` a sa novom uspela — potvrđuje da je nova lozinka sačuvana hashovana. Direktna deaktivacija korisnika u bazi (`is_active = 0`) dok je stari token još uvek u važenju odmah je vratila `403 ACCOUNT_DEACTIVATED` na zaštićenoj ruti, čime je potvrđeno da middleware učitava korisnika iz baze pri svakom zahtevu (FR-014), a ne samo veruje sadržaju tokena; isti nalog je odbijen i pri pokušaju prijave. Baza je posle testa vraćena u čisto stanje sa `npm run db:reset`.
