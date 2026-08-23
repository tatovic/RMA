# 11: Prijava i registracija na uređaju

**What to build:** Čovek uzme telefon, napravi nalog i uđe u aplikaciju. Sledeći put kad je otvori, već je unutra, sve dok mu sesija ne istekne. Ovo je prvi tiket u kojem podatak putuje celim putem: sa ekrana, kroz mrežu, do MySQL baze i nazad.

**Blocked by:** 10 — Mrežni sloj i obrada grešaka.

**Status:** done

- [x] Postoji ulazna aktivnost sa dva ekrana, za prijavu i za registraciju, i prelaz između njih — `ui/AuthenticationActivity.kt` (ACT-1) sa `nav_auth.xml`, `presentation/auth/LoginFragment.kt` (SCR-01) i `RegisterFragment.kt` (SCR-02); prelaz u oba smera uživo proveren na emulatoru
- [x] Registracija traži ime, email, lozinku i potvrdu lozinke; greške se prikazuju ispod odgovarajućeg polja na srpskom (VR-01 do VR-05) — `util/AuthValidator.kt` + `RegisterViewModel.fieldErrors`, sve četiri greške uživo proverene na emulatoru (screenshot sa sve četiri crvene poruke ispod polja)
- [x] Uspešna registracija odmah prijavljuje korisnika, bez dodatnog koraka (FR-018) — `AuthRepository.register()` čuva sesiju i korisnika pri istom pozivu; uživo provereno (`POST /api/auth/register` 201 → direktno `MainActivity`, bez prolaska kroz login)
- [x] Tokom slanja zahteva prikazan je indikator, a dugme je onemogućeno da se ne pošalje dvaput — `ProgressBar`/`isEnabled = !loading` u oba fragmenta, i `ViewModel` odbija novi zahtev dok je `Resource.Loading` u toku
- [x] Zauzet email, pogrešni kredencijali i deaktiviran nalog daju razumljive poruke iz kataloga grešaka — sve tri greške (`EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `ACCOUNT_DEACTIVATED`) uživo izazvane i provereno prikazane tačnim tekstom iz `strings.xml`; `EMAIL_ALREADY_EXISTS` ide ispod polja za email (VR-03), ostale kao opšta poruka
- [x] Bez interneta se prikazuje poruka o nedostatku konekcije, a aplikacija se ne ruši — uživo provereno gašenjem wifi/data na emulatoru; prikazana poruka iz kataloga (`SERVER_UNAVAILABLE`, jer je `BACKEND_BASE_URL` IP adresa pa `UnknownHostException`/`NO_NETWORK` nije dostižna u ovom okruženju — sve tri grane su već pokrivene testovima u `SafeApiCallTest` iz tiketa 10), aplikacija nastavlja da radi
- [x] Token, identifikator korisnika i rola se čuvaju u trajnom lokalnom skladištu; lozinka se ne čuva nigde — `UserPreferences.saveSession()` (DataStore), uživo provereno čitanjem `user_prefs.preferences_pb` sa uređaja (token/user_id/user_role prisutni, lozinka nigde)
- [x] Pri pokretanju aplikacije proverava se važenje sesije i bira se početni ekran (FR-011) — `UserPreferences.hasValidSession()` dekodira JWT `exp` lokalno (`util/JwtUtils.kt`), `MainActivity` rutira na `AuthenticationActivity` ili ostaje; uživo provereno u oba smera
- [x] Posle uspešne prijave povratno dugme ne vraća na ekran prijave — `requireActivity().finish()` posle `startActivity(MainActivity)` u oba fragmenta; uživo provereno (nazad iz `MainActivity` ide na launcher, ne na login)
- [x] Nijedan tekst na ova dva ekrana nije hardkodovan (NFR-09) — svi stringovi u `values/strings.xml`

**Napomena:** Uživo provereno na `Pixel6_API36` emulatoru + lokalnom backend/MySQL (`npm run dev`, baza vraćena u čisto stanje sa `npm run db:reset` posle testa). Registracija, prijava, sve tri poruke grešaka sa servera (zauzet email, pogrešni kredencijali, deaktiviran nalog), validacija svih pet polja, bez-mreže scenario, FR-011 rutiranje u oba smera i back-stack posle prijave — sve potvrđeno na uređaju, ne samo unit testovima. `di/DatabaseModule.kt` je dodat u ovom tiketu (Room baza iz tiketa 09 do sada nije bila povezana u Hilt graf) jer je `AuthRepository` prvi potrošač `UserDao`-a. Unit testovi: `JwtUtilsTest` (4) i `AuthValidatorTest` (6), `gradlew :app:testDebugUnitTest` i `gradlew :app:assembleDebug` BUILD SUCCESSFUL.
