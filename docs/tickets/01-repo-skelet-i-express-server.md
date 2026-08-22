# 01: Repo skelet i Express server

**What to build:** Projekat postaje pokretljiv kostur. Postoji verzionisan repozitorijum povezan sa GitHub-om, sa razdvojenim backend i android delom, i backend server koji se pokreće jednom komandom i odgovara na proveru zdravlja. Od ovog trenutka svaki naredni tiket ima gde da sleti.

**Blocked by:** None (can start immediately).

**Status:** done

### Verzionisanje

- [x] Git repozitorijum je inicijalizovan, `main` grana postoji
- [x] `.gitignore` pokriva `node_modules`, `.env`, Gradle build artefakte i `local.properties`; Room `schemas/` folder NIJE ignorisan
- [x] `README.md` opisuje čemu projekat služi, kako se pokreće i gde je dokumentacija
- [x] Remote `origin` pokazuje na `https://github.com/tatovic/RMA.git`
- [x] Dokumentacija i tiketi su commitovani i push-ovani na `main`
- [x] Provereno da u repou nema nijedne tajne (`.env`, ključevi, `local.properties`)

### Struktura i server

- [x] Folderi `backend/` i `android/` postoje i prate strukturu iz `tech.md` sekcija 3
- [x] Backend se pokreće sa `npm run dev` i restartuje se sam pri izmeni koda
- [x] `GET /api/health` vraća `200` sa `{"status":"ok"}`
- [x] Konfiguracija se čita iz `.env`, a nedostajuća obavezna promenljiva ruši server pri startu sa jasnom porukom
- [x] `.env.example` postoji sa praznim tajnama i commitovan je; `.env` nije u Gitu (GIT-01)
- [x] MySQL connection pool se uspostavlja pri startu i konekcija je u UTC vremenskoj zoni
- [x] Postoji centralni error handler koji je jedino mesto koje formira JSON odgovor sa greškom
- [x] Nepostojeća ruta vraća `404` u standardnom obliku greške iz `prd.md` sekcija 10

**Napomena:** stavke pod „Verzionisanje" su odrađene tokom početnog podešavanja projekta, pre nego što je tiket preuzet. Android folder (`android/`) je za sada prazan skelet — pravi Android projekat i biblioteke dolaze u tiketu 08, u skladu sa njegovim opsegom.
