# 10: Mrežni sloj i obrada grešaka

**What to build:** Aplikacija ume da razgovara sa dva različita servera: sa našim backendom uz token, i sa spoljnim servisom za kurseve bez tokena. Svaka greška, od nestanka mreže do odgovora servera, pretvara se u jedan predvidiv oblik koji viši slojevi znaju da prikažu. Ništa se još ne vidi na ekranu, ali sve je pokriveno testovima.

**Blocked by:** 09 — Lokalna Room baza.

**Status:** done

- [x] Postoje dva odvojena HTTP klijenta; token se šalje isključivo našem backendu, nikada trećoj strani — `di/NetworkModule.kt` (`@Named("backend")` sa `AuthInterceptor`, `@Named("currency")` bez njega)
- [x] Token se automatski dodaje u zaglavlje svakog zahteva ka backendu — `data/remote/interceptor/AuthInterceptor.kt`, token iz `data/local/prefs/UserPreferences.kt` (DataStore)
- [x] Definisani su svi endpointi iz tech.md sekcija 6, kao suspend funkcije — `data/remote/api/BackendApi.kt` (auth, users, items, categories, locations, admin) i `data/remote/api/CurrencyApi.kt` za tech.md sekcija 7; napomena: stvarni backend koristi query parametar `since`, ne `updatedSince` kako piše u tech.md 6.4 — provereno u `backend/src/modules/items/items.schema.js`
- [x] Prenosne klase odgovaraju stvarnom obliku odgovora servera, uključujući razliku u imenovanju polja između JSON-a i Kotlina — DTO-ovi u `data/remote/dto/` prepisani polje-po-polje iz `backend/src/utils/serializer.js`; `ExchangeRatesDto` mapira snake_case (`base_code`, `time_last_update_unix`) u camelCase preko `@SerializedName`
- [x] Postoji mapiranje između prenosnih klasa, entiteta baze i domenskih modela, kao tri odvojene vrste klasa — `data/remote/mapper/` (Item/User/Category/Location/Date), `domain/model/`
- [x] Mapiranje sa servera u bazu prima i čuva postojeću lokalnu putanju do fotografije, koja na serveru ne postoji (DB-RULE-02) — `ItemDto.toEntity(keepImagePath, ...)` u `ItemMapper.kt`, pokriveno testovima u `ItemMapperTest`
- [x] Jedna zajednička funkcija obrađuje svaki mrežni poziv i pretvara ishod u uspeh, grešku ili stanje učitavanja — `util/SafeApiCall.kt`
- [x] Nestanak mreže, istek veze i nedostupan server se razlikuju i daju različite poruke — `UnknownHostException` → `NO_NETWORK`, `SocketTimeoutException` → `TIMEOUT`, ostali `IOException` → `SERVER_UNAVAILABLE` u `SafeApiCall.kt`
- [x] Telo greške sa servera se čita i njegov kod se prevodi u poruku iz kataloga u prd.md sekcija 10 — `parseErrorBody` u `SafeApiCall.kt` + `util/ErrorCode.kt` + `util/AndroidErrorMessageProvider.kt` (`strings.xml`, ERR-02)
- [x] Tekst izuzetka se nikada ne prosleđuje do korisnika, ali se uvek loguje (ERR-03, ERR-04) — `SafeApiCall.kt`, svaka grana radi `Log.e(TAG, ..., e)` i vraća samo tekst iz `ErrorCode`
- [x] Postoje klase stanja za rezultat operacije i za stanje ekrana, sa četiri stanja iz BR-017 — `util/Resource.kt`, `util/UiState.kt`
- [x] Detaljno logovanje mrežnog saobraćaja je uključeno samo u debug build-u — `HttpLoggingInterceptor.Level.BODY`/`NONE` po `BuildConfig.DEBUG` u `NetworkModule.kt`
- [x] Unit testovi pokrivaju mapiranje u oba smera i sve grane obrade grešaka — `data/remote/mapper/*Test.kt` (27 testova) i `util/SafeApiCallTest.kt` (11 testova: uspeh, 204, poznat/nepoznat/neparsljiv kod greške, 401 sa različitim kodovima, sve tri mrežne greške, generalni izuzetak); svih 38 prošlo (`gradlew :app:testDebugUnitTest`), `gradlew :app:assembleDebug` BUILD SUCCESSFUL
