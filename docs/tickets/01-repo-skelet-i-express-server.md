# 01: Repo skelet i Express server

**What to build:** Projekat postaje pokretljiv kostur. Postoji verzionisan repozitorijum sa razdvojenim backend i android delom, i backend server koji se pokreće jednom komandom i odgovara na proveru zdravlja. Od ovog trenutka svaki naredni tiket ima gde da sleti.

**Blocked by:** None (can start immediately).

**Status:** ready-for-agent

- [ ] Git repozitorijum je inicijalizovan, `main` grana postoji
- [ ] `.gitignore` pokriva `node_modules`, `.env`, Gradle build artefakte i `local.properties`; Room `schemas/` folder NIJE ignorisan
- [ ] Struktura foldera odgovara sekciji 3 dokumenta `tech.md`
- [ ] `README.md` opisuje čemu projekat služi i kako se pokreće
- [ ] Backend se pokreće sa `npm run dev` i restartuje se sam pri izmeni koda
- [ ] `GET /api/health` vraća `200` sa `{"status":"ok"}`
- [ ] Konfiguracija se čita iz `.env`, a nedostajuća obavezna promenljiva ruši server pri startu sa jasnom porukom
- [ ] `.env.example` postoji sa praznim tajnama i commitovan je; `.env` nije u Gitu
- [ ] MySQL connection pool se uspostavlja pri startu i konekcija je u UTC vremenskoj zoni
- [ ] Postoji centralni error handler koji je jedino mesto koje formira JSON odgovor sa greškom
- [ ] Nepostojeća ruta vraća `404` u standardnom obliku greške iz `prd.md` sekcija 10
