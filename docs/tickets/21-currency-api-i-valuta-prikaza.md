# 21: Kursna lista i valuta prikaza

**What to build:** Korisnik koji ima laptop kupljen u dinarima i telefon kupljen u evrima dobija jedan broj koji kaže koliko sve to vredi, u valuti koju sam izabere. Kursevi dolaze sa spoljnog servisa, čuvaju se za slučaj da nema interneta, i nikada se ne izmišljaju.

**Blocked by:** 20 — Filteri i sortiranje.

**Status:** ready-for-agent

- [ ] Kursevi se povlače sa spoljnog servisa asinhrono, bez blokiranja korisničkog interfejsa
- [ ] Poziv ide preko zasebnog HTTP klijenta koji ne šalje naš token trećoj strani
- [ ] Odgovor se smatra ispravnim samo ako servis eksplicitno javi uspeh, bez obzira na HTTP status
- [ ] Od 166 valuta koje servis vraća uzima se samo šest podržanih (BR-012)
- [ ] Kursevi se čuvaju lokalno; ako su mlađi od 24 sata, mreža se uopšte ne poziva
- [ ] Bez interneta se koristi poslednji poznati kurs
- [ ] Konverzija je egzaktna i radi nad celim brojevima; nigde se ne koristi decimalni tip za novac (NFR-12)
- [ ] Ako kurs za neku valutu ne postoji ni u kešu, ta valuta se ne računa kao da vredi isto, nego se izdvaja uz jasnu poruku (BR-013)
- [ ] Korisnik bira valutu prikaza u profilu, iz liste od šest podržanih
- [ ] Promena valute prikaza odmah menja sve zbirove u aplikaciji
- [ ] Predmet čija se valuta razlikuje od valute prikaza pokazuje i originalni i preračunati iznos
- [ ] Novac se svuda formatira kroz jedno zajedničko mesto, u srpskom formatu
- [ ] Unit testovi pokrivaju konverziju, nedostajući kurs i zaokruživanje
