# 15: Dodavanje i izmena predmeta

**What to build:** Korisnik može da unese novu stvar u svoj inventar za manje od pola minuta, jer su obavezna samo tri polja. Ako želi da upiše sve detalje, ima gde. Unos radi i kad nema signala, i predmet se odmah vidi u listi.

**Blocked by:** 14 — Lista inventara.

**Status:** ready-for-agent

- [ ] Forma traži obavezno samo naziv, kategoriju i lokaciju (BR-006)
- [ ] Sva ostala polja su u sekciji dodatnih podataka koja je podrazumevano skupljena
- [ ] Kategorija i lokacija se biraju iz padajuće liste popunjene iz lokalne baze
- [ ] Datumi se biraju kroz birač datuma, nikada kucanjem
- [ ] Validacija prati VR-06 do VR-17, uključujući pravilo da datum garancije ne sme biti pre datuma kupovine
- [ ] Cena se unosi u običnom obliku, a čuva se kao ceo broj minor jedinica
- [ ] Prazno opciono polje se čuva kao odsustvo vrednosti, ne kao prazan tekst (BR-016)
- [ ] Identifikator novog predmeta generiše uređaj, pa predmet postoji i pre nego što ga server vidi (FR-029)
- [ ] Čuvanje upisuje u lokalnu bazu i označava predmet kao nesinhronizovan, pa odmah pokušava da ga pošalje serveru
- [ ] Neuspeh slanja ne blokira korisnika; predmet ostaje sačuvan lokalno i čeka na sinhronizaciju
- [ ] Ista forma služi i za izmenu, sa unapred popunjenim vrednostima
- [ ] Izmena osvežava vreme poslednje promene, a vreme kreiranja ostaje netaknuto
- [ ] Napuštanje forme sa nesačuvanim izmenama traži potvrdu
- [ ] Po čuvanju se korisnik vraća na listu i novi predmet je odmah vidljiv, i bez interneta (FR-030)
- [ ] Forma vraća rezultat pozivajućem ekranu, koji prikazuje kratku potvrdu

**Napomena:** ovaj tiket namerno radi samo prosto slanje na server posle upisa. Puna sinhronizacija sa preuzimanjem izmena, konfliktima i ponovnim pokušajima dolazi u tiketu 26 i nadograđuje se na ovo, ne piše se ponovo.
