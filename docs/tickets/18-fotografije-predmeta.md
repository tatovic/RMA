# 18: Fotografije predmeta

**What to build:** Korisnik može da slika ili izabere fotografiju svoje stvari, pa je kasnije prepoznaje na prvi pogled. Slika ostaje uz predmet i onda kad je original obrisan iz galerije, a baza ostaje mala jer se u nju upisuje samo putanja.

**Blocked by:** 17 — Upravljanje lokacijama.

**Status:** done

- [x] Sa forme za unos predmeta korisnik može izabrati fotografiju iz galerije kroz sistemski birač
- [x] Korisnik može i da slika novu fotografiju kamerom (FR-081)
- [x] Izabrana slika se smanjuje na najviše 1080 piksela duže stranice i komprimuje (FR-082)
- [x] Slika se kopira u privatni prostor aplikacije, a u bazu ide samo naziv fajla (FR-083)
- [x] Slika preživljava brisanje originala iz galerije i restart aplikacije
- [x] Fotografija se nikada ne šalje na server (FR-084)
- [x] Slika se prikazuje na stavci u listi i na ekranu detalja
- [x] Zamena fotografije briše prethodni fajl, da se ne gomilaju
- [x] Brisanje predmeta briše i njegov fajl fotografije (FR-086)
- [x] Predmet bez fotografije i dalje prikazuje ikonicu svoje kategorije
- [x] Odbijena dozvola za kameru se obrađuje porukom, bez rušenja aplikacije
