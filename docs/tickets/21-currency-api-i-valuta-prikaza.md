# 21: Kursna lista i valuta prikaza

**What to build:** Korisnik koji ima laptop kupljen u dinarima i telefon kupljen u evrima dobija jedan broj koji kaže koliko sve to vredi, u valuti koju sam izabere. Kursevi dolaze sa spoljnog servisa, čuvaju se za slučaj da nema interneta, i nikada se ne izmišljaju.

**Blocked by:** 20 — Filteri i sortiranje.

**Status:** done

- [x] Kursevi se povlače sa spoljnog servisa asinhrono, bez blokiranja korisničkog interfejsa
- [x] Poziv ide preko zasebnog HTTP klijenta koji ne šalje naš token trećoj strani
- [x] Odgovor se smatra ispravnim samo ako servis eksplicitno javi uspeh, bez obzira na HTTP status
- [x] Od 166 valuta koje servis vraća uzima se samo šest podržanih (BR-012)
- [x] Kursevi se čuvaju lokalno; ako su mlađi od 24 sata, mreža se uopšte ne poziva
- [x] Bez interneta se koristi poslednji poznati kurs
- [x] Konverzija je egzaktna i radi nad celim brojevima; nigde se ne koristi decimalni tip za novac (NFR-12)
- [x] Ako kurs za neku valutu ne postoji ni u kešu, ta valuta se ne računa kao da vredi isto, nego se izdvaja uz jasnu poruku (BR-013)
- [x] Korisnik bira valutu prikaza u profilu, iz liste od šest podržanih
- [x] Promena valute prikaza odmah menja sve zbirove u aplikaciji
- [x] Predmet čija se valuta razlikuje od valute prikaza pokazuje i originalni i preračunati iznos
- [x] Novac se svuda formatira kroz jedno zajedničko mesto, u srpskom formatu
- [x] Unit testovi pokrivaju konverziju, nedostajući kurs i zaokruživanje

Implementacija: `CurrencyConverter` (domain/util) — egzaktna BigDecimal konverzija po tech.md 7.3, HALF_UP
zaokruživanje, `null` (nikad kurs 1.0) kad kurs nedostaje. `CurrencyRepository` — TTL keš od 24h nad
`exchange_rates` (Room), poziva `CurrencyApi` isključivo preko zasebnog `@Named("currency")` OkHttp
klijenta bez `AuthInterceptor`-a (već pripremljeno u `NetworkModule` iz ranijeg tiketa), proverava
`result == "success"` nezavisno od HTTP statusa, filtrira 166→6 valuta (BR-012), pada na keš pri
mrežnoj grešci, a na `CURRENCY_UNAVAILABLE` kad ni kurs ni keš ne postoje (BR-013). `ProfileFragment`
dobija padajući meni „Valuta prikaza" (SUPPORTED_CURRENCIES) koji odmah šalje `PATCH /api/users/me`;
promena se odmah odražava svuda jer svi ekrani čitaju `User.currency` iz istog Room reda.
`DashboardViewModel` sabira zbir po valuti pa svaki konvertuje u valutu prikaza (BR-011 + BR-013 —
nekonvertovane valute se izdvajaju i prikazuju napomenom umesto da se računaju kao 1:1).
`ItemDetailsViewModel` dodaje red „Vrednost u valuti prikaza" (BR-011 efektivna vrednost) kad se
valuta predmeta razlikuje od valute prikaza, sa porukom „Kurs trenutno nije dostupan" ako kurs
nedostaje. Novac i dalje formatira isključivo `MoneyFormatter` (nepromenjen).

Provereno: `./gradlew :app:assembleDebug` i `./gradlew :app:testDebugUnitTest` prolaze (uklj. 8 novih
`CurrencyConverterTest` slučajeva: ista valuta, RSD→EUR i EUR→USD po verifikovanim primerima iz
tech.md, nedostajući kurs izvora/cilja, prazna mapa kurseva, nulti/negativan kurs, HALF_UP zaokruživanje
nadole/na granici/nagore).

Provereno ručno na emulatoru (Pixel6_API36) 2026-08-31, uz pravi backend (`node src/server.js` + MySQL)
i pravi eksterni servis (`open.er-api.com`, bez mokovanja): Profil prikazuje padajući meni „Valuta
prikaza" sa svih šest valuta; promena RSD→EUR odmah trajno sačuvana (PATCH potvrđen, opstala posle
povratka na ekran) i Dashboard ukupna vrednost se **istog trenutka** preračunala sa „5.436.012,83 RSD"
na „46.334,22 EUR" (odnos ≈117,33, poklapa se sa uživo povučenim EUR/RSD kursom) bez ijednog dodatnog
zbira koji nedostaje (nijedna valuta nije ostala nekonvertovana). Detalji predmeta u RSD („Kuhinjske
stolice", nabavna cena 110.700,00 RSD, procenjena vrednost 68.707,00 RSD, količina 4) prikazuju novi red
„Vrednost u valuti prikaza: 2.342,51 EUR" — tačno odgovara BR-011 efektivnoj vrednosti (68.707×4=274.828
RSD) konvertovanoj po istom kursu. Direktan upit nad `exchange_rates` u Room bazi uređaja potvrdio je
tačno šest keširanih redova (`EUR, RSD, USD, CHF, GBP, BAM`, sve `baseCode=EUR`) sa pravim kursevima i
`fetchedAt` vremenskom oznakom — potvrđuje BR-012 filtriranje i keširanje (FR-064). TTL-preskakanje mreže
kad je keš svež i pad na keš bez interneta nisu posebno simulirani u ovoj sesiji (airplane mode/mreža
nije isključivana) — provereno je isključivo kroz implementaciju `CurrencyRepository` i njenu logiku,
ne uživo presretanjem mrežnog saobraćaja.
