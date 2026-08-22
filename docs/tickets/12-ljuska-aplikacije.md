# 12: Ljuska aplikacije i navigacija

**What to build:** Aplikacija dobija svoj stalni oblik: donja navigacija sa četiri odeljka kroz koje korisnik slobodno šeta, i odjava koja ga čisto izbacuje napolje. Ekrani su još prazni, ali kostur navigacije kroz koji će sve naredno da se ukloni je gotov.

**Blocked by:** 11 — Prijava i registracija na uređaju.

**Status:** ready-for-agent

- [ ] Glavna aktivnost sadrži donju navigaciju sa četiri odeljka: pregled, inventar, statistika i profil
- [ ] Postoje sva četiri navigaciona grafa iz tech.md sekcija 10, po jedan za svaku aktivnost
- [ ] Prelazak između odeljaka čuva stanje svakog od njih
- [ ] Donja navigacija je identična za obe role; ništa se u njoj ne skriva ni ne dodaje (SCR-09)
- [ ] Ekrani do kojih se ne dolazi iz menija sakrivaju donju navigaciju dok su otvoreni
- [ ] Profil prikazuje ime, email i rolu prijavljenog korisnika
- [ ] Odjava traži potvrdu kroz dijalog (BR-008)
- [ ] Odjava briše token i kompletan sadržaj lokalne baze, pa vraća na ekran prijave (BR-005)
- [ ] Posle odjave povratno dugme ne vraća u aplikaciju
- [ ] Rotacija ekrana ne ruši navigaciju i ne gubi izabrani odeljak
