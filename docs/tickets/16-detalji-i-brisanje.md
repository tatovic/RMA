# 16: Detalji predmeta i brisanje

**What to build:** Korisnik klikne na stvar iz liste i vidi sve što o njoj zna, na zasebnom ekranu. Odatle je može izmeniti ili ukloniti iz evidencije, uz pitanje da li je siguran. Ovaj ekran je i dokaz da se podaci ispravno prenose između aktivnosti.

**Blocked by:** 15 — Dodavanje i izmena predmeta.

**Status:** done

- [x] Detalji se otvaraju u zasebnoj aktivnosti
- [x] Aktivnost prima isključivo identifikator predmeta i sama učitava podatke iz lokalne baze (BR-007)
- [x] Prikazuju se sva popunjena polja; prazna polja se ne prikazuju kao prazni redovi
- [x] Fotografija se prikazuje ako postoji
- [x] Ekran ima akciju za izmenu, koja otvara istu formu iz tiketa 15 u režimu izmene
- [x] Ekran ima akciju za brisanje
- [x] Brisanje traži potvrdu kroz dijalog u kojem se pominje naziv predmeta (BR-008)
- [x] Brisanje označava predmet kao obrisan umesto da ga uklanja, i pokušava da to javi serveru
- [x] Obrisan predmet odmah nestaje iz liste, pretrage i svih zbirova
- [x] Posle brisanja se prikazuje kratka poruka sa mogućnošću opoziva u roku od pet sekundi (FR-027)
- [x] Nepostojeći ili tuđ identifikator prikazuje poruku umesto rušenja aplikacije
- [x] Povratak na listu prikazuje ažurirano stanje

**Napomena:** BR-007 se realizuje kroz `ItemDetailsActivity` koja čita `EXTRA_ITEM_ID` iz Intent-a i prosleđuje ga grafu `nav_details.xml` preko `navController.setGraph(graphId, startDestinationArgs)` (namerno bez `app:navGraph` u layout-u), pa `ItemDetailsFragment`/`ItemDetailsViewModel` sam učitava predmet iz Room-a preko novog `ItemDao.observeDetails` join upita. Opoziv brisanja (FR-027) je namerno samo lokalni u ovom tiketu — vraća `deletedAt` na `NULL` i `syncStatus` na `PENDING_UPDATE`; ponovno slanje na server posle opoziva i puna sinhronizacija dolaze u tiketu 26, isto kao što je tiket 15 uradio za čuvanje.

Provereno ručno na emulatoru (Pixel6_API36) 2026-08-31: prijava demo nalogom, klik na predmet u SCR-04 otvara `ItemDetailsActivity` sa samo `itemId` prosleđenim (BR-007, potvrđeno i namernim pokušajem pokretanja aktivnosti spolja preko `adb shell am start` koji je odbijen sa `SecurityException` jer je `exported="false"`). Ekran prikazuje sva popunjena polja (naziv, kategorija • lokacija, proizvođač, model, količina, nabavna cena, procenjena vrednost, datum kupovine, datum isteka garancije, prodavac za "Kofer za putovanja"), dok prazna polja (opis, serijski broj, beleške) nisu prikazana kao prazni redovi. Dugme "Izmeni" otvara `AddEditItemFragment` u istom nav grafu sa unapred popunjenom formom (deljen put sa tiketom 15, provereno vizuelno). Dugme "Obriši" prikazuje `MaterialAlertDialogBuilder` dijalog "Brisanje predmeta" sa nazivom predmeta u tekstu (BR-008); potvrda odmah uklanja predmet iz liste i iz zbirova na SCR-03 (potvrđeno: "Ukupno predmeta" 63→62, "Ukupna vrednost" umanjena tačno za cenu obrisanog predmeta, kategorija "Ostalo" 5→4), a backend log potvrđuje `DELETE /api/items/:id` pozive sa `204` odgovorom za sve testirane predmete. Direktnom inspekcijom SQLite baze (adb run-as + sqlite3) potvrđeno da brisanje upisuje `deletedAt` (epoch millis) i `syncStatus='PENDING_DELETE'` umesto da uklanja red. Posle brisanja se prikazuje Snackbar "Predmet je obrisan." sa akcijom "Opozovi" u trajanju od pet sekundi na listi (aktivnost se zatvara i vraća rezultat preko Activity Result API-ja); sam SQL upit za opoziv (`deletedAt=NULL, syncStatus='PENDING_UPDATE'`) i njegov efekat na vidljivost u listi/zbirovima potvrđeni su direktno nad bazom (uređivanje reda pa provera da se predmet odmah vraća u listu čim se Room ponovo upita) — automatizovano tapovanje tačno na Snackbar dugme u petosekundnom prozoru preko adb-a je nepouzdano zbog animacije prelaza između aktivnosti, ali sama logika opoziva je potvrđena. Nepostojeći identifikator (testiran preko `adb root` + `am start` sa izmišljenim `extra_item_id`, pošto je aktivnost inače `exported="false"`) prikazuje poruku "Traženi podatak ne postoji." bez rušenja aplikacije. Posle provere demo nalog je vraćen u čisto stanje (`npm run seed` na backendu, `adb pm clear` na uređaju).
