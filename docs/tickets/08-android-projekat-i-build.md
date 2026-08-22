# 08: Android projekat i build konfiguracija

**What to build:** Prazna Android aplikacija se pokreće na emulatoru. Sve biblioteke su na mestu, tema je postavljena, a build koristi ispravnu verziju Jave. Ovo je dosadan ali neophodan temelj — svaki naredni Android tiket pretpostavlja da build prolazi.

**Blocked by:** 07 — Demo inventar i Postman kolekcija.

**Status:** ready-for-agent

- [ ] Projekat se otvara i sinhronizuje u Android Studiju bez grešaka
- [ ] Gradle koristi JDK isporučen uz Android Studio, ne Javu iz sistemskog PATH-a, vidi tech.md sekcija 1
- [ ] Minimalna podržana verzija Androida je API 26 (NFR-13)
- [ ] Sve biblioteke iz tech.md sekcija 2.2 su dodate i razrešene; verzije označene sa proveriti su potvrđene pri prvom sinhronizovanju
- [ ] JitPack repozitorijum je dodat, jer biblioteka za grafikone dolazi odatle
- [ ] ViewBinding je uključen, Compose nije prisutan nigde u projektu
- [ ] Material 3 tema je postavljena, sa definisanim svetlim i tamnim varijantama (NFR-10)
- [ ] Adrese backenda i eksternog servisa za kurseve dolaze iz build konfiguracije, ne iz koda
- [ ] Konfiguracija mrežne bezbednosti dozvoljava nešifrovan saobraćaj isključivo ka lokalnim razvojnim adresama (SEC-11)
- [ ] Aplikacija ima dozvolu za pristup internetu i pokreće se na emulatoru sa praznim ekranom
- [ ] Emulator sa sistemskom slikom je kreiran i radi
