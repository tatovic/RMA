# Home Inventory — Tehnička arhitektura (TECH)

| Polje | Vrednost |
|---|---|
| Verzija dokumenta | 1.0 |
| Datum | 2026-08-22 |
| Status | Odobreno — osnova za implementaciju |
| Prateći dokumenti | `prd.md` (zahtevi), `db.md` (model podataka) |
| Repozitorijum | Mono-repo: `backend/` + `android/` |

---

## 0. Kako agenti koriste ovaj dokument

Ovo je **jedini izvor istine za KAKO se sistem gradi**. Pravila:

1. **Ne uvodi nove biblioteke** bez odobrenja korisnika. Stack u sekciji 2 je zatvoren.
2. **Ne menjaj API kontrakt** iz sekcije 6 — Android i backend agenti rade paralelno i oslanjaju se na njega.
3. **Poštuj pravila zavisnosti između slojeva** iz sekcije 8. Kršenje je odbijeno na code review-u.
4. Sve **verzije biblioteka označene sa (proveriti)** moraju se potvrditi pri prvom build-u; ne pinuj ih naslepo.
5. Konvencije iz sekcije 15 važe za sav kod koji bilo koji agent napiše.

---

## 1. Verifikovano stanje razvojne mašine

Provereno 2026-08-22 direktnom inspekcijom sistema. Agenti mogu računati na sledeće:

| Alat | Stanje | Putanja / verzija |
|---|---|---|
| Android Studio | instaliran | `D:\1\bin\studio64.exe`, verzija 2026.1.3 |
| Bundled JBR | dostupan | `D:\1\jbr` — OpenJDK **25.0.2** |
| Android SDK | instaliran | `C:\Users\tatov\AppData\Local\Android\Sdk` |
| SDK platforms | **samo android-37.0** | drugi API leveli se preuzimaju kroz SDK Manager |
| SDK build-tools | 36.0.0 | |
| Emulator binarni fajl | postoji | `Sdk\emulator\emulator.exe` |
| **System images** | **NIJEDNA** | **blokator — mora se preuzeti** |
| **cmdline-tools** | **nedostaju** | potrebni za `sdkmanager` iz konzole |
| AVD-ovi | nijedan kreiran | |
| Node.js | instaliran | v22.16.0 |
| npm | instaliran | 11.4.2 |
| MySQL Server | instaliran, **servis radi** | `C:\Program Files\MySQL\MySQL Server 8.0\bin`, servis `MySQL80` |
| MySQL Workbench | instaliran | `C:\Program Files\MySQL\MySQL Workbench 8.0` |
| Git | instaliran | 2.45.1 |
| Java u PATH-u | 20 | **ne koristiti za Gradle** — vidi upozorenje ispod |
| Docker | nije instaliran | nije potreban |

### Radnje potrebne pre početka Faze 3

| # | Radnja | Kako |
|---|---|---|
| 1 | Preuzeti system image za emulator | Studio → Device Manager → Create Device → System Image → Google APIs, API 36, x86_64 (~2 GB) |
| 2 | Kreirati AVD | Preporuka: Pixel 7, API 36, 4 GB RAM |
| 3 | Postaviti Gradle JDK | Studio → Settings → Build Tools → Gradle → Gradle JDK = **bundled JBR (25)**, ne Java 20 iz PATH-a |
| 4 | Dodati MySQL u PATH (opciono) | `C:\Program Files\MySQL\MySQL Server 8.0\bin` — za `mysql` iz konzole |
| 5 | Zabeležiti MySQL root lozinku | Potrebna za `backend/.env`; poznata je samo korisniku |

**Upozorenje o Javi:** u `PATH`-u je Java 20, koju Android Gradle Plugin **ne podržava**. Gradle mora koristiti bundled JBR 25 iz `D:\1\jbr`. Ako build prijavi grešku o nepodržanoj verziji Jave, proveriti podešavanje iz tačke 3, a ne menjati AGP verziju.

---

## 2. Tehnološki stack

### 2.1 Backend

| Tehnologija | Uloga | Verzija |
|---|---|---|
| Node.js | Runtime | 22.16.0 (instalirana) |
| Express | HTTP framework | 4.x |
| mysql2 | MySQL drajver, `promise` API | 3.x |
| jsonwebtoken | Izdavanje i verifikacija JWT | 9.x |
| bcrypt | Hashovanje lozinki, cost 10 | 5.x (proveriti; ako native build pukne na Windows-u → `bcryptjs`) |
| zod | Validacija ulaza | 3.x |
| dotenv | Konfiguracija iz `.env` | 16.x |
| cors | CORS | 2.8.5 |
| helmet | Bezbednosni HTTP zaglavlja | 7.x (proveriti) |
| morgan | Logovanje zahteva | 1.x |
| uuid | Generisanje UUID v4 na serveru | 9.x |
| nodemon | Auto-restart u razvoju | devDependency |

**Napomena o `bcrypt`:** paket zahteva native kompilaciju. Ako `npm install` pukne na Windows-u zbog nedostatka build alata, prelazi se na **`bcryptjs`** — čist JavaScript, isti API, isti format hasha, samo sporiji. Ovo je unapred odobrena zamena i ne zahteva novo pitanje korisniku.

### 2.2 Android

| Tehnologija | Uloga | Verzija |
|---|---|---|
| Kotlin | Jezik | 2.x (proveriti — iz Studio šablona) |
| Android Gradle Plugin | Build | proveriti iz šablona |
| compileSdk / targetSdk | 37 | jedina instalirana platforma |
| minSdk | **26** | NFR-13 |
| ViewBinding | Pristup View-ovima | ugrađeno u AGP |
| Material Components | Material 3 UI | 1.12.0 |
| Navigation Component | Navigacija između fragmenata | 2.8.x |
| Lifecycle / ViewModel | Stanje UI-ja | 2.8.x |
| Room | Lokalna baza | 2.6.x / 2.7.x (proveriti) |
| DataStore Preferences | Token, podešavanja | 1.1.1 |
| Retrofit | HTTP klijent | 2.11.0 |
| OkHttp | Mreža, interceptori | 4.12.0 |
| Gson (converter-gson) | JSON | 2.11.0 |
| Hilt | Dependency injection | 2.5x (proveriti) |
| KSP | Generisanje koda za Room i Hilt | uskladiti sa Kotlin verzijom |
| Coroutines | Asinhroni rad | 1.8.x |
| Glide | Učitavanje slika | 4.16.0 |
| MPAndroidChart | Grafikoni | v3.1.0 (JitPack) |
| SwipeRefreshLayout | Pull-to-refresh | 1.1.0 |

**Zabranjeno bez odobrenja:** Jetpack Compose, RxJava, Koin, Moshi, Coil, Paging, WorkManager, bilo koji drugi DI ili HTTP klijent.

**MPAndroidChart zahteva JitPack repozitorijum** u `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2.3 Eksterni API

| Stavka | Vrednost |
|---|---|
| Servis | `open.er-api.com` (ExchangeRate-API, besplatni otvoreni endpoint) |
| Base URL | `https://open.er-api.com/v6/` |
| Endpoint | `GET latest/EUR` |
| API ključ | **nije potreban** |
| Osvežavanje | jednom dnevno na strani servisa |
| Broj valuta u odgovoru | 166 (koristi se 6, BR-012) |

Odgovor je **verifikovan pravim pozivom** 2026-08-22; struktura je u sekciji 7.

---

## 3. Struktura repozitorijuma

```
RMA/
├── prd.md
├── db.md
├── tech.md
├── README.md
├── .gitignore
│
├── backend/
│   ├── src/
│   │   ├── config/
│   │   │   ├── env.js            # ucitavanje i validacija .env
│   │   │   └── db.js             # mysql2 connection pool
│   │   ├── middleware/
│   │   │   ├── authenticate.js   # verifikacija JWT, punjenje req.user
│   │   │   ├── requireAdmin.js   # provera role
│   │   │   ├── validate.js       # zod middleware
│   │   │   ├── errorHandler.js   # centralna obrada gresaka
│   │   │   └── notFound.js
│   │   ├── modules/
│   │   │   ├── auth/             # .routes .controller .service .schema
│   │   │   ├── users/
│   │   │   ├── items/
│   │   │   ├── categories/
│   │   │   ├── locations/
│   │   │   ├── statistics/
│   │   │   └── admin/
│   │   ├── utils/
│   │   │   ├── AppError.js
│   │   │   ├── errorCodes.js
│   │   │   ├── asyncHandler.js
│   │   │   └── serializer.js     # DATETIME <-> ISO-8601
│   │   ├── db/
│   │   │   ├── schema.sql
│   │   │   ├── migrations/
│   │   │   ├── createDb.js
│   │   │   └── seed.js
│   │   ├── app.js                # Express aplikacija
│   │   └── server.js             # pokretanje
│   ├── .env.example
│   ├── .env                      # NIJE u Git-u
│   ├── package.json
│   └── postman/
│       └── HomeInventory.postman_collection.json
│
└── android/
    ├── settings.gradle.kts
    ├── build.gradle.kts
    ├── gradle/libs.versions.toml
    └── app/
        ├── build.gradle.kts
        ├── schemas/                       # Room exportSchema
        └── src/main/
            ├── AndroidManifest.xml
            ├── res/
            │   ├── layout/
            │   ├── navigation/            # nav_auth, nav_main, nav_details, nav_admin
            │   ├── values/strings.xml     # SVI tekstovi, NFR-09
            │   ├── values/colors.xml
            │   ├── values/themes.xml
            │   ├── menu/bottom_nav_menu.xml
            │   ├── drawable/
            │   └── xml/network_security_config.xml
            └── java/rs/homeinventory/app/
                ├── HomeInventoryApp.kt
                ├── di/
                │   ├── DatabaseModule.kt
                │   ├── NetworkModule.kt
                │   ├── RepositoryModule.kt
                │   └── AppModule.kt
                ├── data/
                │   ├── local/
                │   │   ├── HomeInventoryDatabase.kt
                │   │   ├── Converters.kt
                │   │   ├── entity/
                │   │   ├── dao/
                │   │   └── prefs/UserPreferences.kt
                │   ├── remote/
                │   │   ├── api/BackendApi.kt
                │   │   ├── api/CurrencyApi.kt
                │   │   ├── dto/
                │   │   ├── interceptor/AuthInterceptor.kt
                │   │   └── mapper/
                │   ├── repository/
                │   └── sync/SyncManager.kt
                ├── domain/
                │   ├── model/
                │   ├── usecase/
                │   └── util/
                │       ├── WarrantyCalculator.kt
                │       ├── CurrencyConverter.kt
                │       └── MoneyFormatter.kt
                ├── presentation/
                │   ├── auth/
                │   ├── dashboard/
                │   ├── inventory/
                │   ├── itemdetails/
                │   ├── additem/
                │   ├── statistics/
                │   ├── profile/
                │   ├── locations/
                │   ├── admin/
                │   └── common/
                ├── ui/                    # Activity klase
                └── util/
                    ├── Resource.kt
                    ├── UiState.kt
                    ├── NetworkMonitor.kt
                    ├── ImageStorage.kt
                    └── Constants.kt
```

---

## 4. Konfiguracija okruženja

### `backend/.env.example`

```
NODE_ENV=development
PORT=3000

DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=
DB_NAME=home_inventory
DB_CONNECTION_LIMIT=10

JWT_SECRET=
JWT_EXPIRES_IN=7d

BCRYPT_ROUNDS=10

CORS_ORIGIN=*
```

`DB_PASSWORD` i `JWT_SECRET` popunjava korisnik. `JWT_SECRET` mora biti nasumičnih najmanje 32 bajta:

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

**`.env` nikada ne ide u Git.** `.env.example` ide, sa praznim tajnama.

### Base URL na Androidu

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "BACKEND_BASE_URL", "\"http://10.0.2.2:3000/api/\"")
        buildConfigField("String", "CURRENCY_BASE_URL", "\"https://open.er-api.com/v6/\"")
    }
    buildFeatures { viewBinding = true; buildConfig = true }
}
```

| Cilj | Base URL |
|---|---|
| Emulator (izabrano) | `http://10.0.2.2:3000/api/` |
| Fizički telefon na istoj mreži | `http://<LAN-IP-racunara>:3000/api/` |

`10.0.2.2` je alias emulatora za `localhost` host mašine. `localhost` unutar emulatora pokazuje na sam emulator i **neće raditi**.

### `network_security_config.xml`

Cleartext HTTP je dozvoljen isključivo za lokalne razvojne adrese (NFR-08):

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

Poveže se u manifestu: `android:networkSecurityConfig="@xml/network_security_config"`.
Dozvole: `<uses-permission android:name="android.permission.INTERNET" />`.

---

## 5. Arhitektura

### 5.1 Slojevi

```
   FRAGMENT / ACTIVITY  (View)
        |  observe StateFlow            ^ korisnicki dogadjaji
        v                               |
   VIEWMODEL
        |  poziva suspend funkcije      ^ Flow<T> / Resource<T>
        v                               |
   REPOSITORY   <--- jedina tacka pristupa podacima
        |                               |
        +-------------+-----------------+
        v             v                 v
   ROOM DAO      RETROFIT           DATASTORE
   (lokalno)     (backend +          (token,
                  currency API)      podesavanja)
```

### 5.2 Pravila zavisnosti — obavezujuća

| # | Pravilo |
|---|---|
| DEP-01 | Fragment **nikada** ne poziva DAO, Retrofit ni DataStore direktno |
| DEP-02 | ViewModel **nikada** ne poziva DAO ni Retrofit direktno — samo Repository ili UseCase |
| DEP-03 | Repository **nikada** ne zna za Android View klase, `Context` (osim `@ApplicationContext`), ni za Fragmente |
| DEP-04 | `domain/` paket **ne sme** importovati ništa iz `android.*` osim `androidx.annotation` |
| DEP-05 | Entity, DTO i Domain model su **tri različite klase**; mapiranje ide isključivo kroz `data/remote/mapper/` |
| DEP-06 | UI prikazuje **isključivo** domain modele, nikada Entity ni DTO |
| DEP-07 | Nijedan poziv baze ili mreže ne sme se izvršiti na `Dispatchers.Main` |

### 5.3 Offline-first tok čitanja

**Room je jedini izvor koji UI posmatra.** Mreža samo puni Room.

```
Fragment  ->  ViewModel  ->  Repository.observeItems() : Flow<List<Item>>
                                    |
                                    +-- Room DAO Flow  ------> UI se azurira ODMAH
                                    |
                                    +-- (paralelno) SyncManager.sync()
                                              |
                                              +-- upise u Room  --> Flow ponovo emituje --> UI se osvezava
```

Posledica: ekran se **nikada ne prazni** dok se čeka mreža, i radi identično offline.

### 5.4 Klase stanja

```kotlin
// util/Resource.kt — rezultat jedne operacije
sealed interface Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val code: ErrorCode, val message: String) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}

// util/UiState.kt — stanje celog ekrana (BR-017)
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
```

Svaki ViewModel izlaže `StateFlow<UiState<X>>`. Fragment ga prikuplja preko `repeatOnLifecycle(Lifecycle.State.STARTED)` — **nikada** golim `collect` u `onViewCreated`.

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { render(it) }
    }
}
```

---

## 6. API kontrakt

Prefiks svih ruta: `/api`. Sadržaj: `application/json; charset=utf-8`.

### 6.1 Oblik odgovora

Uspeh — resurs ili niz direktno:
```json
{ "id": "550e8400-...", "name": "Samsung TV", "quantity": 1 }
```

Greška — uvek isti oblik:
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Neispravni podaci",
    "details": [ { "field": "email", "message": "Neispravan format" } ]
  }
}
```

`details` postoji samo kod `VALIDATION_ERROR`. Kompletan katalog kodova je u `prd.md`, sekcija 10.

### 6.2 Autentifikacija

| Metod | Putanja | Auth | Opis |
|---|---|:---:|---|
| POST | `/api/auth/register` | ne | Registracija |
| POST | `/api/auth/login` | ne | Prijava |

**POST `/api/auth/register`**
```json
// zahtev
{ "name": "Marko", "email": "marko@primer.rs", "password": "Lozinka123", "confirmPassword": "Lozinka123" }

// 201
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": { "id": "…", "name": "Marko", "email": "marko@primer.rs",
            "role": "ADMIN", "isActive": true, "currency": "RSD",
            "createdAt": "2026-08-22T14:07:31.123Z" }
}
```
Server u **jednoj transakciji**: kreira korisnika, dodeljuje rolu po BR-001, kreira devet lokacija po BR-015.
Greške: `VALIDATION_ERROR` 400, `EMAIL_ALREADY_EXISTS` 409.

**POST `/api/auth/login`**
```json
// zahtev
{ "email": "marko@primer.rs", "password": "Lozinka123" }
// 200 — isti oblik kao register
```
Greške: `INVALID_CREDENTIALS` 401 (i za nepostojeći email i za pogrešnu lozinku, FR-017), `ACCOUNT_DEACTIVATED` 403.

### 6.3 Korisnik

| Metod | Putanja | Auth | Opis |
|---|---|:---:|---|
| GET | `/api/users/me` | da | Profil prijavljenog korisnika |
| PATCH | `/api/users/me` | da | Izmena imena i/ili valute prikaza |
| POST | `/api/users/me/password` | da | Promena lozinke |

**PATCH `/api/users/me`** — telo: `{ "name": "Marko M.", "currency": "EUR" }` (oba polja opciona).
**POST `/api/users/me/password`** — telo: `{ "currentPassword": "…", "newPassword": "…" }`. Greška `WRONG_CURRENT_PASSWORD` 400.

### 6.4 Predmeti

| Metod | Putanja | Auth | Opis |
|---|---|:---:|---|
| GET | `/api/items` | da | Lista; podržava `updatedSince` i `includeDeleted` |
| GET | `/api/items/:id` | da | Jedan predmet |
| POST | `/api/items` | da | Kreiranje (klijent šalje `id`) |
| PUT | `/api/items/:id` | da | Puna izmena |
| DELETE | `/api/items/:id` | da | Soft delete |

**GET `/api/items`**

| Query parametar | Tip | Podrazumevano | Opis |
|---|---|---|---|
| `updatedSince` | ISO-8601 | — | Delta pull (FR-098); vraća i tombstone redove |
| `includeDeleted` | boolean | `false` | Uključi soft-obrisane; automatski `true` uz `updatedSince` |
| `limit` | number | 500 | Maksimum 500 |

```json
// 200
{
  "items": [ { "id": "…", "name": "Samsung TV", "…": "…", "deletedAt": null } ],
  "serverTime": "2026-08-22T14:07:31.123Z"
}
```

`serverTime` klijent čuva kao `items_last_sync_at` za sledeći delta pull. **Koristi se serversko vreme, ne vreme uređaja** — sat na telefonu može biti pogrešan.

**POST `/api/items`** — telo sadrži **sva** polja iz mapiranja u `db.md` sekcija 7, uključujući `id` koji generiše klijent. Server **ignoriše** `userId` iz tela (OWN-02) i uzima ga iz tokena.

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Samsung TV 55\"",
  "description": null,
  "categoryId": "…", "locationId": "…",
  "manufacturer": "Samsung", "model": "UE55TU7092", "serialNumber": "SN123456",
  "quantity": 1,
  "purchasePrice": 8990000, "estimatedValue": null, "currency": "RSD",
  "purchaseDate": "2024-03-15", "warrantyExpirationDate": "2026-03-15",
  "seller": "Tehnomanija", "notes": null,
  "createdAt": "2026-08-22T14:07:31.123Z",
  "updatedAt": "2026-08-22T14:07:31.123Z"
}
```

Odgovori: `201` sa kreiranim predmetom. Ako `id` već postoji → `200` sa postojećim (idempotentnost pri ponovljenom sync-u). Greške: `VALIDATION_ERROR` 400, `NOT_FOUND` 404 (kategorija ili lokacija ne postoji / nije korisnikova, OWN-04).

**PUT `/api/items/:id`** — telo isto kao POST, plus obavezan `updatedAt` klijenta za LWW proveru (DB-RULE-04).
- `200` prihvaćeno
- `409 SYNC_CONFLICT` sa serverskom verzijom u `error.details.serverVersion`
- `404 NOT_FOUND` ako predmet nije korisnikov (OWN-03)

**DELETE `/api/items/:id`** — postavlja `deleted_at = NOW(3)`. Odgovor `204`. Brisanje već obrisanog je takođe `204` (idempotentno).

### 6.5 Kategorije i lokacije

| Metod | Putanja | Auth | Rola | Opis |
|---|---|:---:|---|---|
| GET | `/api/categories` | da | bilo koja | Lista globalnih kategorija |
| POST | `/api/categories` | da | ADMIN | Kreiranje |
| PUT | `/api/categories/:id` | da | ADMIN | Izmena |
| DELETE | `/api/categories/:id` | da | ADMIN | Brisanje, blokirano po BR-014 |
| GET | `/api/locations` | da | bilo koja | Lokacije prijavljenog korisnika |
| POST | `/api/locations` | da | bilo koja | Kreiranje |
| PUT | `/api/locations/:id` | da | bilo koja | Izmena |
| DELETE | `/api/locations/:id` | da | bilo koja | Brisanje, blokirano po BR-014 |

`GET /api/categories` vraća i `itemCount` za prikaz u administraciji.
Greške pri brisanju: `CATEGORY_IN_USE` / `LOCATION_IN_USE` 409, sa brojem predmeta u `error.details.count`.

### 6.6 Administracija

| Metod | Putanja | Rola | Opis |
|---|---|---|---|
| GET | `/api/admin/statistics` | ADMIN | Sistemski agregati |
| GET | `/api/admin/users` | ADMIN | Lista korisnika |
| PATCH | `/api/admin/users/:id/status` | ADMIN | `{ "isActive": false }` |

`GET /api/admin/users` vraća isključivo: `id`, `name`, `email`, `role`, `isActive`, `createdAt`, `itemCount`. **Nikada sadržaj inventara** (OWN-06).
`PATCH .../status` na sopstveni nalog → `409 CANNOT_DEACTIVATE_SELF`.

### 6.7 Middleware lanac

```
helmet -> cors -> express.json({limit:'1mb'}) -> morgan
   -> [rute bez auth: /api/auth/*]
   -> authenticate  (verifikuje JWT, ucita korisnika, proveri is_active)
   -> [requireAdmin za /api/admin/*]
   -> ruter modula
   -> notFound
   -> errorHandler   (jedini koji formira JSON gresku)
```

**`authenticate` mora učitati korisnika iz baze pri svakom zahtevu** i proveriti `is_active`. Provera samo JWT payload-a nije dovoljna — deaktivacija bi tada delovala tek za sedam dana (FR-014).

**`errorHandler` je jedino mesto koje formira odgovor sa greškom.** Nijedan kontroler ne šalje grešku direktno. U `production` režimu se stack trace ne šalje klijentu nikada.

---

## 7. Eksterni Currency API

### 7.1 Verifikovani odgovor

`GET https://open.er-api.com/v6/latest/EUR` — provereno 2026-08-22, HTTP 200:

```json
{
  "result": "success",
  "provider": "https://www.exchangerate-api.com",
  "documentation": "…",
  "terms_of_use": "…",
  "time_last_update_unix": 1787356951,
  "time_last_update_utc": "Sat, 22 Aug 2026 00:02:31 +0000",
  "time_next_update_unix": 1787444461,
  "time_next_update_utc": "Sun, 23 Aug 2026 00:21:01 +0000",
  "time_eol_unix": 0,
  "base_code": "EUR",
  "rates": { "EUR": 1, "RSD": 117.328652, "USD": 1.16819,
             "CHF": 0.935579, "GBP": 0.856021, "BAM": 1.95583, "…": 0 }
}
```

`rates` sadrži 166 valuta; koristi se šest iz BR-012. Vrednosti su JSON brojevi.

### 7.2 DTO i API interfejs

```kotlin
data class ExchangeRatesDto(
    @SerializedName("result")                val result: String,
    @SerializedName("base_code")             val baseCode: String,
    @SerializedName("time_last_update_unix") val timeLastUpdateUnix: Long,
    @SerializedName("time_next_update_unix") val timeNextUpdateUnix: Long,
    @SerializedName("rates")                 val rates: Map<String, Double>
)

interface CurrencyApi {
    @GET("latest/{base}")
    suspend fun getRates(@Path("base") base: String = "EUR"): Response<ExchangeRatesDto>
}
```

`result` mora biti `"success"`; svaka druga vrednost se tretira kao greška bez obzira na HTTP status.

### 7.3 Algoritam konverzije

Kursevi su EUR-bazirani: `rates[X]` = koliko jedinica valute X vredi 1 EUR.

Konverzija iznosa iz valute `FROM` u valutu `TO`:

```
iznosTO = iznosFROM * rates[TO] / rates[FROM]
```

Implementacija mora biti egzaktna (NFR-12):

```kotlin
object CurrencyConverter {
    fun convert(amountMinor: Long, from: String, to: String, rates: Map<String, Double>): Long? {
        if (from == to) return amountMinor
        val rFrom = rates[from] ?: return null   // BR-013: null, nikada 1.0
        val rTo   = rates[to]   ?: return null
        if (rFrom <= 0.0 || rTo <= 0.0) return null
        return BigDecimal.valueOf(amountMinor)
            .multiply(BigDecimal.valueOf(rTo))
            .divide(BigDecimal.valueOf(rFrom), 0, RoundingMode.HALF_UP)
            .toLong()
    }
}
```

Pošto sve podržane valute imaju eksponent 2, minor jedinice se preslikavaju direktno bez skaliranja.

**Provera:** 120.000,00 RSD → EUR. `12000000 * 1.0 / 117.328652 = 102277` minor jedinica = **1.022,77 EUR**. Poklapa se sa primerom iz specifikacije („120.000 RSD ≈ 1.020 EUR").

### 7.4 Keširanje

| Pravilo | Vrednost |
|---|---|
| TTL | 24 sata |
| Skladište | Room tabela `exchange_rates` |
| Ponašanje kad je keš svež | koristi keš, **bez** mrežnog poziva |
| Ponašanje kad je keš zastareo | pokušaj mreže; pri neuspehu koristi stari keš |
| Ponašanje kad keša nema i mreža ne radi | `Resource.Error`, primeni BR-013 |
| Prikaz datuma kursa | **ne prikazuje se** (odluka korisnika) |

---

## 8. Ključni algoritmi na Androidu

### 8.1 Status garancije (BR-010)

```kotlin
object WarrantyCalculator {
    fun status(expiration: String?, thresholdDays: Int, today: LocalDate = LocalDate.now()): WarrantyStatus {
        val exp = expiration?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return WarrantyStatus.NEPOZNATO
        return when {
            exp.isBefore(today)                            -> WarrantyStatus.ISTEKLA
            !exp.isAfter(today.plusDays(thresholdDays.toLong())) -> WarrantyStatus.USKORO_ISTICE
            else                                           -> WarrantyStatus.AKTIVNA
        }
    }

    fun daysRemaining(expiration: String, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, LocalDate.parse(expiration))
}
```

`LocalDate` zahteva API 26+ — zadovoljeno sa minSdk 26 (NFR-13).

### 8.2 Formatiranje novca

```kotlin
object MoneyFormatter {
    private val locale = Locale("sr", "RS")
    private const val EXPONENT = 2

    fun format(amountMinor: Long, currency: String): String {
        val major = BigDecimal.valueOf(amountMinor).movePointLeft(EXPONENT)
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
        return "${nf.format(major)} $currency"   // "120.000,00 RSD"
    }
}
```

**Nijedan drugi deo koda ne sme formatirati novac.** Zabranjeno je `String.format("%.2f", …)` nad novčanim vrednostima.

### 8.3 Efektivna vrednost (BR-011)

```kotlin
fun InventoryItem.effectiveValueMinor(): Long =
    (estimatedValue ?: purchasePrice ?: 0L) * quantity
```

### 8.4 Ukupna vrednost u valuti prikaza

```kotlin
fun totalValue(aggregates: List<CategoryAggregate>, display: String, rates: Map<String, Double>): TotalValueResult {
    var total = 0L
    val unconvertible = mutableMapOf<String, Long>()   // BR-013
    aggregates.forEach { agg ->
        val converted = CurrencyConverter.convert(agg.totalMinor, agg.currency, display, rates)
        if (converted != null) total += converted
        else unconvertible.merge(agg.currency, agg.totalMinor, Long::plus)
    }
    return TotalValueResult(total, display, unconvertible)
}
```

Ako `unconvertible` nije prazan, UI prikazuje zbir **i** poruku „Kurs trenutno nije dostupan" sa izdvojenim iznosima.

### 8.5 Pretraga sa debounce-om (FR-032)

```kotlin
private val query = MutableStateFlow("")

val items: StateFlow<UiState<List<ItemUi>>> =
    combine(
        query.debounce(300).distinctUntilChanged().map { it.trim().lowercase(Locale("sr","RS")) },
        filters,
        sortOrder
    ) { q, f, s -> Triple(q, f, s) }
     .flatMapLatest { (q, f, s) -> repository.observeItems(q, f, s) }
     .map { rows -> if (rows.isEmpty()) UiState.Empty else UiState.Success(rows.toUi()) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
```

Upit se **normalizuje u Kotlinu** (`trim` + `lowercase` sa srpskim `Locale`) pre slanja u SQL, jer SQLite `COLLATE NOCASE` ne pokriva Č, Ć, Š, Ž, Đ. SQL poredi nad `LOWER(kolona)`.

### 8.6 Sinhronizacija

```kotlin
suspend fun sync(): Resource<Unit> = withContext(Dispatchers.IO) {
    if (!networkMonitor.isOnline()) return@withContext Resource.Error(ErrorCode.NO_NETWORK, "…")

    // ---- 1. PUSH ----
    itemDao.getPending().forEach { item ->
        when (item.syncStatus) {
            SyncStatus.PENDING_CREATE -> api.createItem(item.toDto()).onSuccess {
                itemDao.setSyncStatus(item.id, SyncStatus.SYNCED)
            }
            SyncStatus.PENDING_UPDATE -> api.updateItem(item.id, item.toDto())
                .onSuccess { itemDao.setSyncStatus(item.id, SyncStatus.SYNCED) }
                .onConflict { server -> itemDao.upsert(server.toEntity(keepImagePath = item.imagePath)) }
            SyncStatus.PENDING_DELETE -> api.deleteItem(item.id).onSuccess {
                imageStorage.delete(item.imagePath)     // FR-086
                itemDao.hardDelete(item.id)
            }
            SyncStatus.SYNCED -> Unit
        }
    }

    // ---- 2. PULL (delta) ----
    val since = syncMetadataDao.get(KEY_ITEMS_LAST_SYNC)
    val response = api.getItems(updatedSince = since) ?: return@withContext Resource.Error(...)
    response.items.forEach { dto ->
        val local = itemDao.getById(dto.id)
        if (local != null && local.syncStatus != SyncStatus.SYNCED) return@forEach  // lokalne izmene imaju prednost do push-a
        if (dto.deletedAt != null) {
            imageStorage.delete(local?.imagePath); itemDao.hardDelete(dto.id)
        } else {
            itemDao.upsert(dto.toEntity(keepImagePath = local?.imagePath))          // DB-RULE-02
        }
    }
    syncMetadataDao.put(KEY_ITEMS_LAST_SYNC, response.serverTime)                   // serversko vreme
    Resource.Success(Unit)
}
```

**Tri pravila koja se ne smeju prekršiti:**
1. Push ide **pre** pull-a, inače bi serverska verzija pregazila lokalne izmene.
2. `keepImagePath` se prosleđuje **pri svakom** mapiranju DTO → Entity (DB-RULE-02, FR-085).
3. Za `items_last_sync_at` se koristi `serverTime` iz odgovora, nikad `System.currentTimeMillis()`.

Sinhronizacija se pokreće iz `viewModelScope`; neuspeh je tih (log + eventualno Snackbar) i nikada ne menja UI u stanje greške ako lokalni podaci postoje (FR-097).

---

## 9. Hilt moduli

```kotlin
@HiltAndroidApp class HomeInventoryApp : Application()

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): HomeInventoryDatabase =
        Room.databaseBuilder(ctx, HomeInventoryDatabase::class.java, "home_inventory.db").build()

    @Provides fun itemDao(db: HomeInventoryDatabase) = db.itemDao()
    // … ostali DAO-ovi
}

@Module @InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun authInterceptor(prefs: UserPreferences) = AuthInterceptor(prefs)

    @Provides @Singleton @Named("backend")
    fun backendClient(auth: AuthInterceptor, log: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(auth).addInterceptor(log)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("currency")
    fun currencyClient(log: HttpLoggingInterceptor): OkHttpClient =   // BEZ AuthInterceptor-a
        OkHttpClient.Builder().addInterceptor(log)
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

    @Provides @Singleton
    fun backendApi(@Named("backend") c: OkHttpClient): BackendApi =
        Retrofit.Builder().baseUrl(BuildConfig.BACKEND_BASE_URL).client(c)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(BackendApi::class.java)

    @Provides @Singleton
    fun currencyApi(@Named("currency") c: OkHttpClient): CurrencyApi =
        Retrofit.Builder().baseUrl(BuildConfig.CURRENCY_BASE_URL).client(c)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(CurrencyApi::class.java)
}
```

**Dva odvojena OkHttp klijenta su obavezna.** Slanje našeg JWT tokena na `open.er-api.com` bilo bi curenje kredencijala ka trećoj strani.

`HttpLoggingInterceptor` sme biti `Level.BODY` samo u debug build-u; u release-u `Level.NONE`.

### AuthInterceptor

```kotlin
class AuthInterceptor(private val prefs: UserPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { prefs.token.first() }
        val request = if (token.isNullOrBlank()) chain.request()
        else chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}
```

`runBlocking` je ovde prihvatljiv jer se interceptor izvršava na OkHttp niti, nikada na glavnoj.

**Obrada `401` (FR-009)** se radi u `Repository` sloju, ne u interceptoru: prazni se DataStore, briše Room baza i emituje se `SessionExpired` događaj koji `MainActivity` prevodi u prelazak na `AuthenticationActivity`.

---

## 10. Navigacija

Četiri odvojena navigaciona grafa, po jedan za svaku aktivnost:

| Graf | Aktivnost | Destinacije |
|---|---|---|
| `nav_auth.xml` | ACT-1 | SCR-01 Login (start), SCR-02 Register |
| `nav_main.xml` | ACT-2 | SCR-03 Dashboard (start), SCR-04 Inventory, SCR-08 Statistics, SCR-09 Profile, SCR-06 AddEditItem, SCR-10 Locations |
| `nav_details.xml` | ACT-3 | SCR-07 ItemDetails (start), SCR-06 AddEditItem |
| `nav_admin.xml` | ACT-4 | SCR-11 AdminDashboard (start), SCR-12 AdminUsers, SCR-13 AdminCategories |

### Prenos podataka

**Između aktivnosti** — Intent extra sa konstantom:
```kotlin
// util/Constants.kt
const val EXTRA_ITEM_ID = "extra_item_id"

// pokretanje
Intent(requireContext(), ItemDetailsActivity::class.java)
    .putExtra(EXTRA_ITEM_ID, item.id)
    .let { startActivity(it) }
```

**Između fragmenata** — Safe Args:
```xml
<fragment android:id="@+id/addEditItemFragment" …>
    <argument android:name="itemId" app:argType="string" app:nullable="true"
              android:defaultValue="@null" />
</fragment>
```
`itemId == null` znači režim dodavanja, inače režim izmene.

**Nikada se ne prosleđuje ceo objekat predmeta** (BR-007).

### Bottom navigacija

`BottomNavigationView` + `setupWithNavController(navController)`. Četiri stavke. `SCR-06` i `SCR-10` nisu u meniju — do njih se dolazi iz `SCR-04` i `SCR-09`, a bottom bar se na njima sakriva preko `addOnDestinationChangedListener`.

---

## 11. Obrada grešaka na Androidu

```kotlin
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T> = try {
    val r = call()
    when {
        r.isSuccessful && r.body() != null -> Resource.Success(r.body()!!)
        r.code() == 401 -> Resource.Error(ErrorCode.TOKEN_INVALID, "…")
        else -> parseErrorBody(r)      // cita { error: { code, message } }
    }
} catch (e: UnknownHostException)  { Resource.Error(ErrorCode.NO_NETWORK, "…") }
  catch (e: SocketTimeoutException){ Resource.Error(ErrorCode.TIMEOUT, "…") }
  catch (e: IOException)           { Resource.Error(ErrorCode.NO_NETWORK, "…") }
  catch (e: Exception)             { Log.e(TAG, "Neocekivana greska", e)
                                     Resource.Error(ErrorCode.UNKNOWN, "…") }
```

| Pravilo | Detalj |
|---|---|
| ERR-01 | `catch (e: Exception)` postoji **samo** u `safeApiCall`; nigde drugde se ne guta izuzetak |
| ERR-02 | Korisniku se prikazuje isključivo tekst iz `strings.xml`, mapiran preko `ErrorCode` |
| ERR-03 | `e.message` se **nikada** ne prikazuje korisniku |
| ERR-04 | Svaka uhvaćena greška se loguje u Logcat sa tagom klase |
| ERR-05 | Greška mreže kad lokalni podaci postoje → Snackbar, ne `UiState.Error` |

---

## 12. Bezbednost

| ID | Mera | Gde |
|---|---|---|
| SEC-01 | BCrypt cost 10 | backend, `auth.service.js` |
| SEC-02 | JWT HS256, tajna iz `.env`, minimum 32 bajta | backend |
| SEC-03 | JWT sadrži samo `sub`, `role`, `iat`, `exp` | backend |
| SEC-04 | `is_active` se proverava iz baze pri svakom zahtevu | `authenticate.js` |
| SEC-05 | Ownership filter u svakom upitu | svi item/location upiti |
| SEC-06 | Tuđi resurs vraća `404`, ne `403` | OWN-03 |
| SEC-07 | Validacija na obe strane; serverska je obavezna | zod + Android |
| SEC-08 | Parametrizovani SQL upiti — **nikada** konkatenacija stringova | `mysql2` prepared statements |
| SEC-09 | `helmet` bezbednosna zaglavlja | `app.js` |
| SEC-10 | Lozinka se ne loguje, ne kešira i ne čuva na uređaju | svuda |
| SEC-11 | Cleartext HTTP samo za `10.0.2.2` i `localhost` | `network_security_config.xml` |
| SEC-12 | `.env` i `local.properties` u `.gitignore` | repo |
| SEC-13 | Rate limiting na `/api/auth/*` — 10 pokušaja / 15 min po IP | opciono, Faza 7 |

**Poznato ograničenje koje se dokumentuje, ne krije:** u razvoju se koristi HTTP bez TLS-a, pa je JWT vidljiv na lokalnoj mreži. Produkcijsko okruženje zahteva HTTPS. Ovo je svesna razvojna odluka (NFR-08) i navodi se u README-u.

---

## 13. Strategija testiranja

Obim je namerno ograničen — cilj je pokrivenost logike koja se lako pokvari, ne procenat linija.

| Nivo | Šta se testira | Alat |
|---|---|---|
| Unit (Android) | `WarrantyCalculator` — sve četiri grane BR-010, granični slučajevi | JUnit 4 |
| Unit (Android) | `CurrencyConverter` — konverzija, nedostajući kurs, zaokruživanje | JUnit 4 |
| Unit (Android) | `MoneyFormatter` — RSD i EUR format | JUnit 4 |
| Unit (Android) | Mapiranje DTO ↔ Entity ↔ Domain, posebno `keepImagePath` | JUnit 4 |
| Instrumentirani | DAO upiti: pretraga po 6 polja, agregacija, soft delete filter | Room in-memory + AndroidJUnit4 |
| Instrumentirani | Room migracije | `MigrationTestHelper` |
| Ručno (Postman) | Svaki endpoint: uspeh, validacija, auth, ownership | Postman kolekcija u repou |
| Ručno | Scenariji iz `prd.md` sekcija 14 | acceptance checklist |

**Obavezni test slučajevi ownership-a u Postman-u:** prijava kao korisnik A, kreiranje predmeta, prijava kao korisnik B, `GET /api/items/<id korisnika A>` → **mora vratiti `404`**.

---

## 14. Git strategija

| Stavka | Odluka |
|---|---|
| Repozitorijum | Jedan (mono-repo), koren je `RMA/` |
| **Remote** | **`https://github.com/tatovic/RMA.git`** |
| **Vidljivost** | **javan (public)** — vidi upozorenje ispod |
| Glavna grana | `main` — uvek u stanju koje se builduje |
| Radne grane | `feat/<oblast>-<opis>`, npr. `feat/inventory-search` |
| Format commita | `<tip>(<oblast>): <opis> [<ID zahteva>]` |
| Tipovi | `feat`, `fix`, `refactor`, `docs`, `chore`, `test` |
| Primer | `feat(auth): registracija sa BCrypt hashovanjem [FR-001][FR-003]` |
| Tagovi | `v0.1-faza2`, `v0.2-faza3` … na kraju svake faze |
| Push | Posle svakog završenog tiketa, zajedno sa štikliranjem u `prd.md` sekcija 16 |

### Repozitorijum je javan — šta to znači u praksi

Sve što se commituje je vidljivo svakome na internetu, i **ostaje u istoriji Git-a i posle brisanja**. Zato važe sledeća pravila, bez izuzetka:

| ID | Pravilo |
|---|---|
| GIT-01 | `.env` se **nikada** ne commituje. U repo ide samo `.env.example` sa praznim vrednostima. |
| GIT-02 | `JWT_SECRET`, lozinka MySQL-a i bilo koji drugi tajni podatak ne smeju se pojaviti ni u kodu, ni u dokumentaciji, ni u commit poruci. |
| GIT-03 | Ako tajna slučajno ode u commit, **nije dovoljno obrisati je narednim commitom** — mora se rotirati (generisati nova) i prijaviti korisniku. |
| GIT-04 | Demo kredencijali iz seed skripte smeju u repo, jer se odnose na lokalnu bazu bez stvarnih podataka. |
| GIT-05 | Screenshot-ovi i logovi se pre commita provere da ne sadrže token iz `Authorization` zaglavlja. |
| GIT-06 | `local.properties` sadrži apsolutne putanje sa ove mašine i ne commituje se. |

### `.gitignore` — obavezne stavke

```
# Backend
node_modules/
.env
npm-debug.log*

# Android
android/.gradle/
android/local.properties
android/build/
android/app/build/
*.iml
.idea/
*.keystore
*.jks

# OS
Thumbs.db
desktop.ini
```

**`android/app/schemas/` se NE ignoriše** — Room JSON šeme moraju biti u Git-u da bi se pisale migracije.

---

## 15. Konvencije kodiranja

### Opšte

| # | Pravilo |
|---|---|
| C-01 | Kod, nazivi klasa, promenljivih, tabela i kolona — **engleski** |
| C-02 | Tekst vidljiv korisniku — **srpski**, isključivo u `strings.xml` (NFR-09) |
| C-03 | Bez hardkodovanih stringova u layoutima i Kotlin kodu |
| C-04 | Bez „magičnih brojeva" — konstante u `Constants.kt` ili `dimens.xml` |
| C-05 | Komentari objašnjavaju **zašto**, ne **šta** |
| C-06 | Svako poslovno pravilo u kodu referencira svoj ID: `// BR-011` |

### Kotlin

| # | Pravilo |
|---|---|
| K-01 | `val` po pravilu, `var` samo kad je nužno |
| K-02 | Nullable tipovi se rešavaju eksplicitno; `!!` je zabranjen osim uz komentar sa obrazloženjem |
| K-03 | Data klase za modele, sealed interface za stanja |
| K-04 | Extension funkcije za mapiranje: `fun ItemDto.toEntity(): InventoryItemEntity` |
| K-05 | Fragment koristi `_binding` / `binding` obrazac i **mora** postaviti `_binding = null` u `onDestroyView` |
| K-06 | Svaka `suspend` funkcija koja dodiruje bazu ili mrežu ide preko `withContext(Dispatchers.IO)` |
| K-07 | `GlobalScope` je zabranjen; koristi se `viewModelScope` |

### Nazivi fajlova

| Tip | Obrazac | Primer |
|---|---|---|
| Entity | `<Naziv>Entity.kt` | `InventoryItemEntity.kt` |
| DAO | `<Naziv>Dao.kt` | `ItemDao.kt` |
| DTO | `<Naziv>Dto.kt` | `ExchangeRatesDto.kt` |
| Domain model | `<Naziv>.kt` | `InventoryItem.kt` |
| Repository | `<Naziv>Repository.kt` | `ItemRepository.kt` |
| ViewModel | `<Ekran>ViewModel.kt` | `InventoryViewModel.kt` |
| Fragment | `<Ekran>Fragment.kt` | `InventoryFragment.kt` |
| Layout | `fragment_<ekran>.xml`, `item_<naziv>.xml` | `fragment_inventory.xml` |

### Backend

| # | Pravilo |
|---|---|
| B-01 | Modul ima četiri fajla: `.routes.js`, `.controller.js`, `.service.js`, `.schema.js` |
| B-02 | Kontroler nikada ne piše SQL — to radi servis |
| B-03 | Svaki async handler je umotan u `asyncHandler` |
| B-04 | Greške se bacaju kao `AppError(code, httpStatus, details)` |
| B-05 | Svaki SQL upit je parametrizovan (SEC-08) |
| B-06 | Operacije koje menjaju više tabela idu u transakciju |

---

## 16. Pokretanje projekta

### Backend

```bash
cd backend
npm install
cp .env.example .env
npm run db:create
npm run seed
npm run dev
```

Provera: `curl http://localhost:3000/api/health` → `{"status":"ok"}`.

### Android

1. Otvoriti `D:\1\bin\studio64.exe`
2. Open → `C:\Users\tatov\OneDrive\Desktop\RMA\android`
3. Gradle JDK → bundled JBR (25)
4. Sync Gradle
5. Preuzeti system image i kreirati AVD (sekcija 1)
6. Run

### Redosled provere pri prvom pokretanju

| # | Provera | Očekivano |
|---|---|---|
| 1 | MySQL servis radi | `sc query MySQL80` → `RUNNING` |
| 2 | Backend odgovara | `/api/health` vraća `200` |
| 3 | Emulator vidi backend | Logcat pokazuje `200` na `/api/auth/login` |
| 4 | Room baza kreirana | Studio → App Inspection → Database Inspector |
| 5 | Currency API dostupan | Logcat pokazuje `result: success` |

---

## 17. Poznati rizici

| Rizik | Verovatnoća | Ublažavanje |
|---|---|---|
| `bcrypt` native build pukne na Windows-u | srednja | Prelazak na `bcryptjs`, unapred odobreno |
| Java 20 iz PATH-a se koristi za Gradle | visoka | Eksplicitno postaviti Gradle JDK na JBR 25 |
| Preuzimanje system image-a traje / nema prostora | srednja | Alternativa: fizički telefon preko USB-a i LAN IP |
| compileSdk 37 nije podržan u AGP verziji iz šablona | niska | Preuzeti API 36 kroz SDK Manager i spustiti compileSdk |
| Verzije biblioteka označene sa (proveriti) ne postoje | srednja | Rešava se pri prvom Gradle sync-u, ne pinovati naslepo |
| SQLite `NOCASE` ne pokriva srpske dijakritike | visoka | Normalizacija upita u Kotlinu + `LOWER()` u SQL-u (8.5) |
| Sat na uređaju pogrešan → LWW greši | niska | Za `lastSyncAt` se koristi `serverTime` iz odgovora |

---

**Kraj dokumenta.** Izmene zahtevaju odobrenje korisnika i podizanje verzije u zaglavlju.
