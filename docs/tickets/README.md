# Tiketi — Home Inventory

Indeks. Svaki tiket živi u sopstvenom fajlu; ovde su samo linkovi i redosled.

Lanac je **strogo linearan**: tiket N je blokiran tiketom N-1. Radi se odozgo nadole, jedan po jedan.

Izvor: `prd.md`, `db.md`, `tech.md` u korenu projekta. Tiketi ne ponavljaju te dokumente nego ih referenciraju preko oznaka (FR-xxx, BR-xxx, VR-xx, OWN-xx, DB-RULE-xx).

## Faza 2 — Backend jezgro

| # | Tiket |
|---|---|
| 01 | [Repo skelet i Express server](01-repo-skelet-i-express-server.md) |
| 02 | [MySQL šema i globalne kategorije](02-mysql-sema-i-kategorije.md) |
| 03 | [Registracija i prijava](03-registracija-i-prijava.md) |
| 04 | [Zaštita ruta i korisnički profil](04-auth-middleware-i-profil.md) |
| 05 | [CRUD predmeta sa proverom vlasništva](05-crud-predmeta.md) |
| 06 | [Kategorije i lokacije preko API-ja](06-kategorije-i-lokacije.md) |
| 07 | [Demo inventar i Postman kolekcija](07-demo-seed-i-postman.md) |

## Faza 3 — Android vertikalni presek

| # | Tiket |
|---|---|
| 08 | [Android projekat i build konfiguracija](08-android-projekat-i-build.md) |
| 09 | [Lokalna Room baza](09-room-baza.md) |
| 10 | [Mrežni sloj i obrada grešaka](10-mrezni-sloj.md) |
| 11 | [Prijava i registracija na uređaju](11-prijava-i-registracija-android.md) |
| 12 | [Ljuska aplikacije i navigacija](12-ljuska-aplikacije.md) |
| 13 | [Pregled stanja i zatvaranje vertikalnog preseka](13-dashboard.md) |

## Faza 4 — CRUD inventara

| # | Tiket |
|---|---|
| 14 | [Lista inventara](14-lista-inventara.md) |
| 15 | [Dodavanje i izmena predmeta](15-dodavanje-i-izmena-predmeta.md) |
| 16 | [Detalji predmeta i brisanje](16-detalji-i-brisanje.md) |
| 17 | [Upravljanje lokacijama](17-upravljanje-lokacijama.md) |
| 18 | [Fotografije predmeta](18-fotografije-predmeta.md) |

## Faza 5 — Pretraga i filteri

| # | Tiket |
|---|---|
| 19 | [Pretraga inventara](19-pretraga.md) |
| 20 | [Filteri i sortiranje](20-filteri-i-sortiranje.md) |

## Faza 6 — Vrednost i analitika

| # | Tiket |
|---|---|
| 21 | [Kursna lista i valuta prikaza](21-currency-api-i-valuta-prikaza.md) |
| 22 | [Praćenje garancija](22-pracenje-garancija.md) |
| 23 | [Statistika sa grafikonima](23-statistika.md) |

## Faza 7 — Administracija, offline, poliranje

| # | Tiket |
|---|---|
| 24 | [Administratorski deo API-ja](24-backend-admin-endpointi.md) |
| 25 | [Administratorski ekrani](25-admin-ekrani.md) |
| 26 | [Puna sinhronizacija i offline rad](26-sinhronizacija.md) |
| 27 | [Poliranje i provera projektnih zahteva](27-poliranje-i-acceptance.md) |

## Faza 8 — Popravke

| # | Tiket |
|---|---|
| 28 | [Popravke posle revizije koda](28-popravke-posle-revizije.md) |

## Ključne tačke

- **Tiket 13** zatvara prvi kompletan vertikalni presek: podatak putuje od MySQL baze do ekrana telefona.
- **Tiket 18** je granica MVP-a. Posle njega postoji odbranjiv proizvod.
- **Tiket 15** radi samo prosto slanje na server; **tiket 26** to nadograđuje u punu sinhronizaciju. To nije duplo pisanje nego namerno proširenje.
- **Tiket 26** je najrizičniji u projektu i zato je izolovan.

## Praćenje napretka

Status svakog tiketa se vodi na **dva mesta i oba se ažuriraju u istom commitu**:

1. **U samom fajlu tiketa** — kriterijumi se štikliraju (`- [ ]` postaje `- [x]`), a `**Status:** ready-for-agent` se menja u `**Status:** done`.
2. **U [prd.md, sekcija 16](../../prd.md#16-napredak-implementacije)** — zvanična tabela napretka, sa datumom i hash-om commita.

Puna procedura i pravila štikliranja (TRK-01 do TRK-07) su u `prd.md` sekcija 16. Pročitaj ih pre preuzimanja bilo kog tiketa.

Najvažnije pravilo: **tiket se štiklira tek kada su svi njegovi kriterijumi ispunjeni i stvarno provereni.** Delimično završen posao ostaje neštikliran, uz napomenu šta nedostaje.
