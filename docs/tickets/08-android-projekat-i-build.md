# 08: Android projekat i build konfiguracija

**What to build:** Prazna Android aplikacija se pokreće na emulatoru. Sve biblioteke su na mestu, tema je postavljena, a build koristi ispravnu verziju Jave. Ovo je dosadan ali neophodan temelj — svaki naredni Android tiket pretpostavlja da build prolazi.

**Blocked by:** 07 — Demo inventar i Postman kolekcija.

**Status:** done

- [x] Projekat se otvara i sinhronizuje u Android Studiju bez grešaka — verifikovano komandnolinijskim `gradle wrapper` + `gradlew :app:assembleDebug` (BUILD SUCCESSFUL), pošto Android Studio GUI nije dostupan agentu u ovoj sesiji; potrebna kratka provera u samom Studiju
- [x] Gradle koristi JDK isporučen uz Android Studio, ne Javu iz sistemskog PATH-a, vidi tech.md sekcija 1
- [x] Minimalna podržana verzija Androida je API 26 (NFR-13)
- [x] Sve biblioteke iz tech.md sekcija 2.2 su dodate i razrešene; verzije označene sa proveriti su potvrđene pri prvom sinhronizovanju (stvarne dostupne verzije se razlikuju od nagađanja u tech.md, vidi napomenu)
- [x] JitPack repozitorijum je dodat, jer biblioteka za grafikone dolazi odatle
- [x] ViewBinding je uključen, Compose nije prisutan nigde u projektu
- [x] Material 3 tema je postavljena, sa definisanim svetlim i tamnim varijantama (NFR-10)
- [x] Adrese backenda i eksternog servisa za kurseve dolaze iz build konfiguracije, ne iz koda
- [x] Konfiguracija mrežne bezbednosti dozvoljava nešifrovan saobraćaj isključivo ka lokalnim razvojnim adresama (SEC-11)
- [x] Aplikacija ima dozvolu za pristup internetu i pokreće se na emulatoru sa praznim ekranom — potvrđeno na emulatoru: instalirana, pokrenuta, `pidof` stabilan preko vremena, screenshot pokazuje prazan ekran u boji Material 3 svetle teme, bez FATAL grešaka u logcat-u
- [x] Emulator sa sistemskom slikom je kreiran i radi — AVD `Pixel6_API36` (system image `android-36;google_apis;x86_64`), radi stabilno

### Napomena — odstupanja i status

**Hardverska akceleracija:** korisnik je ručno uključio Windows Hypervisor Platform (WHPX) i restartovao mašinu; `emulator.exe` je potom prijavio "WHPX ... operational" i pokrenuo x86_64 sliku bez problema.

**Promena sistemske slike (API 37.0 → API 36):** prvi AVD (`Pixel6_API37`, `android-37.0;google_apis;x86_64`) je kreiran i pokretao se, ali je `system_server` u gostu ulazio u petlju restartovanja (menjao PID na svakih par sekundi, nezavisno od slobodne RAM memorije na hostu), zbog čega je instalacija aplikacije bila nepouzdana ("Activity class does not exist" / "Can't find service: activity"), a `adb screencap` je padao na internoj GPU asertaciji. Ovo je ponašanje same preview sistemske slike (`android-37.0`), ne projekta. Rešeno prelaskom na `system-images;android-36;google_apis;x86_64` — stabilnu, dugo dostupnu sliku — tačno po mitigaciji koju tech.md sekcija 17 već predviđa za slučaj da najnovija platforma pravi probleme. Aplikacija i dalje kompajlira sa `compileSdk`/`targetSdk` 37; samo AVD za ručnu proveru koristi API 36 (targetSdk viši od runtime API nivoa emulatora je podržan i uobičajen).

**Host resursi:** tokom provere je host mašina bila pod pritiskom memorije (ostale aplikacije korisnika); korisnik je zatvorio deo aplikacija da oslobodi RAM pre finalne, stabilne provere.

**Odstupanja od tech.md (očekivana, jer su verzije označene "proveriti"):**
- AGP: `9.3.1` (najnovija stabilna u trenutku rada), ne "proveriti iz šablona"
- Kotlin: `2.3.21`, KSP: `2.3.11` — birano po kompatibilnosti sa AGP 9.3.1 kroz stvarni build, ne naslepo
- Hilt: `2.60.1` (tech.md je nagađao "2.5x")
- Room: `2.7.2`, Navigation: `2.9.8` (novije od tech.md pretpostavke `2.8.x`, jer `2.8.x` safeargs plugin ne radi sa AGP 9.0+ novim DSL-om)
- AGP 9.0 ukida potrebu za zasebnim `org.jetbrains.kotlin.android` pluginom, ali KSP (Room/Hilt/Glide) to još ne podržava, pa je u `gradle.properties` dodato `android.builtInKotlin=false` i `android.newDsl=false` da bi se zadržao tradicionalni tok dok se alati ne uklope

Sve navedeno je verifikovano stvarnim Gradle build-om (ne pretpostavkom), u skladu sa tech.md pravilom "ne pinuj naslepo".
