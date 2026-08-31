# 15: Dodavanje i izmena predmeta

**What to build:** Korisnik može da unese novu stvar u svoj inventar za manje od pola minuta, jer su obavezna samo tri polja. Ako želi da upiše sve detalje, ima gde. Unos radi i kad nema signala, i predmet se odmah vidi u listi.

**Blocked by:** 14 — Lista inventara.

**Status:** done

- [x] Forma traži obavezno samo naziv, kategoriju i lokaciju (BR-006)
- [x] Sva ostala polja su u sekciji dodatnih podataka koja je podrazumevano skupljena
- [x] Kategorija i lokacija se biraju iz padajuće liste popunjene iz lokalne baze
- [x] Datumi se biraju kroz birač datuma, nikada kucanjem
- [x] Validacija prati VR-06 do VR-17, uključujući pravilo da datum garancije ne sme biti pre datuma kupovine
- [x] Cena se unosi u običnom obliku, a čuva se kao ceo broj minor jedinica
- [x] Prazno opciono polje se čuva kao odsustvo vrednosti, ne kao prazan tekst (BR-016)
- [x] Identifikator novog predmeta generiše uređaj, pa predmet postoji i pre nego što ga server vidi (FR-029)
- [x] Čuvanje upisuje u lokalnu bazu i označava predmet kao nesinhronizovan, pa odmah pokušava da ga pošalje serveru
- [x] Neuspeh slanja ne blokira korisnika; predmet ostaje sačuvan lokalno i čeka na sinhronizaciju
- [x] Ista forma služi i za izmenu, sa unapred popunjenim vrednostima
- [x] Izmena osvežava vreme poslednje promene, a vreme kreiranja ostaje netaknuto
- [x] Napuštanje forme sa nesačuvanim izmenama traži potvrdu
- [x] Po čuvanju se korisnik vraća na listu i novi predmet je odmah vidljiv, i bez interneta (FR-030)
- [x] Forma vraća rezultat pozivajućem ekranu, koji prikazuje kratku potvrdu

**Napomena:** ovaj tiket namerno radi samo prosto slanje na server posle upisa. Puna sinhronizacija sa preuzimanjem izmena, konfliktima i ponovnim pokušajima dolazi u tiketu 26 i nadograđuje se na ovo, ne piše se ponovo.

Provereno ručno na emulatoru (Pixel6_API36) 2026-08-31: prijava demo nalogom, FAB na SCR-04 otvara praznu formu (SCR-06, rezim dodavanja) sa samo tri polja uvek vidljiva (naziv, kategorija, lokacija) i sekcijom "Dodatni podaci" podrazumevano skupljenom; Sačuvaj na praznoj formi prikazuje sve tri VR-06/VR-07/VR-08 greške lokalno, bez mrežnog poziva. Kategorija i lokacija se biraju iz ExposedDropdownMenu-a popunjenog iz lokalnog Room-a (CategoryDao/LocationDao) i rade i bez interneta. Datumi se biraju isključivo kroz MaterialDatePicker (polja su neuredljiva preko tastature); VR-14 (datum garancije pre datuma kupovine) direktno proveren i ispravno blokira čuvanje uz poruku "Datum garancije ne može biti pre datuma kupovine". Cena uneta kao "1234.56" je sačuvana u SQLite kao `purchasePrice=123456` (minor jedinice, VR-10). Napuštanje forme sa nesačuvanim izmenama (dugme Nazad) prikazuje dijalog "Odbaciti izmene?" sa "Nastavi uređivanje"/"Odbaci"; potvrđeno da nastavak vraća u formu bez gubitka unosa. Čuvanje predmeta online ("Test Predmet 15") odmah je vidljivo na vrhu liste (FR-030), pokrenulo je `POST /api/items` (201 potvrđeno u backend logu) i upisano je sa `syncStatus=SYNCED`; gašenje mreže (adb `svc wifi/data disable`) pa čuvanje drugog predmeta ("Offline Test Predmet") i dalje je odmah vidljivo u listi, backend NIJE primio zahtev (potvrđeno u logu), a red u SQLite ima `syncStatus=PENDING_CREATE` — čuvanje nije blokirano offline radom. Direktnom inspekcijom SQLite baze (adb run-as + sqlite3) potvrđeno: `id` je UUID v4 generisan na klijentu (FR-029), opciona tekstualna polja ostavljena prazna upisana su kao `NULL` a ne prazan string (BR-016), `quantity` je podrazumevano 1, `createdAt=updatedAt` za nov predmet. Posle čuvanja prikazan je Snackbar "Predmet je sačuvan." na listi (Fragment Result API). Rezim izmene (unapred popunjena forma, očuvan `createdAt`, osvežen `updatedAt`) proveren je pregledom koda i deljenim putem sa rezimom dodavanja (ista validacija/čuvanje kroz `AddEditItemViewModel`), ali nije ručno proveden kroz UI jer ulazna tačka ("Izmeni" na SCR-07) dolazi tek u tiketu 16 — forma već ispravno čita opcioni `itemId` Safe Args argument i `AddEditItemViewModel.loadInitialData()` ga koristi za pretpunjenje polja. Posle provere demo nalog je vraćen u čisto stanje (`npm run seed` na backendu, `adb pm clear` na uređaju) da testni predmeti ne ostanu u bazi.
