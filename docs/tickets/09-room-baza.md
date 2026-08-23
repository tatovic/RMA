# 09: Lokalna Room baza

**What to build:** Aplikacija dobija sopstvenu bazu na uređaju, mesto koje će biti jedini izvor istine za sve što se prikazuje na ekranu. Upiti za listu, pretragu, garancije i statistiku već postoje i pokriveni su testovima, iako ih još niko ne prikazuje.

**Blocked by:** 08 — Android projekat i build konfiguracija.

**Status:** done

- [x] Svih šest entiteta iz db.md sekcija 5.1 postoji sa tačnim tipovima — `UserEntity`, `CategoryEntity`, `LocationEntity`, `InventoryItemEntity`, `ExchangeRateEntity`, `SyncMetadataEntity`, prepisani polje-po-polje iz db.md
- [x] Kalendarski datumi se čuvaju kao tekst u formatu godina-mesec-dan, trenuci kao ceo broj milisekundi, novac kao ceo broj minor jedinica
- [x] Entitet predmeta ima i dva polja kojih na serveru nema: `imagePath` i `syncStatus`
- [x] Indeksi iz db.md sekcija 5.1 su postavljeni — identični `indices` blokovi na `LocationEntity` i `InventoryItemEntity`
- [x] Upit za listu spaja predmet sa kategorijom i lokacijom i vraća projekciju spremnu za prikaz — `ItemDao.observeAll` → `ItemListRow`
- [x] Upit za pretragu pokriva svih šest polja iz FR-031 — `ItemDao.search`; koristi `LOWER()` poređenje (ne `COLLATE NOCASE`) jer normalizacija upita ide u Kotlin sloju po odluci iz tech.md 8.5 (srpski dijakritici)
- [x] Upit za garancije vraća predmete kojima datum ističe u zadatom rasponu, sortirane po hitnosti — `ItemDao.observeExpiringWarranties`, `ORDER BY warrantyExpirationDate ASC`
- [x] Upit za agregaciju grupiše po kategoriji i po valuti, jer se iznosi u različitim valutama ne smeju sabirati u SQL-u (BR-009) — `ItemDao.observeCategoryAggregates`
- [x] Svi upiti za prikaz izostavljaju obrisane predmete — `deletedAt IS NULL` u svim čitajućim upitima
- [x] Postoji upit koji vraća sve predmete koji čekaju sinhronizaciju — `ItemDao.getPending`
- [x] Postoji operacija koja briše kompletan sadržaj baze, za potrebe odjave (BR-005) — `HomeInventoryDatabase.clearAllData()`
- [x] Šema se izvozi u JSON i taj folder je commitovan u repozitorijum — `android/app/schemas/rs.homeinventory.app.data.local.HomeInventoryDatabase/1.json`, generisan pravim `gradlew :app:assembleDebug` (BUILD SUCCESSFUL)
- [x] Instrumentirani testovi nad bazom u memoriji potvrđuju pretragu, agregaciju i izostavljanje obrisanih predmeta — `ItemDaoTest` (5 testova), pokrenut na emulatoru AVD `Pixel6_API36` preko `gradlew :app:connectedDebugAndroidTest` — svih 5 prošlo
