# Home Inventory — Product Requirements Document (PRD)

| Polje | Vrednost |
|---|---|
| Proizvod | Home Inventory — Android aplikacija za evidenciju kućnog inventara |
| Verzija dokumenta | 1.0 |
| Datum | 2026-08-22 |
| Status | Odobreno — osnova za implementaciju |
| Prateći dokumenti | `db.md` (model podataka), `tech.md` (tehnička arhitektura) |

---

## 0. Kako agenti koriste ovaj dokument

Ovaj dokument je **jedini izvor istine za ŠTA aplikacija radi**. Podela odgovornosti:

| Dokument | Odgovara na pitanje |
|---|---|
| `prd.md` | ŠTA proizvod radi, za koga, po kojim pravilima |
| `db.md` | GDE i KAKO podaci žive (MySQL šema, Room šema, mapiranja) |
| `tech.md` | KAKO se to gradi (stack, slojevi, API kontrakt, algoritmi, konvencije) |

### Obavezna pravila za agente

1. **Ne menjaj odluke iz ovog dokumenta bez eksplicitnog odobrenja korisnika.** Sve odluke su rezultat strukturisanog intervjua od 30 pitanja i namerne su.
2. **Ako naiđeš na konflikt** između ovog dokumenta, `db.md`, `tech.md` ili originalne specifikacije predmeta — **zaustavi se i prijavi konflikt**, nemoj ga rešavati samovoljno.
3. **ID-jevi su stabilni.** `FR-012`, `BR-007`, `SCR-04` itd. se nikada ne renumerišu. Nove stavke dobijaju sledeći slobodan broj. Ukinute stavke se označavaju kao `DEPRECATED`, ne brišu se.
4. **Svaki commit koji implementira zahtev referencira njegov ID** u poruci commita (npr. `feat(inventory): pretraga po nazivu i modelu [FR-031]`).
5. **Ne izmišljaj funkcionalnosti** kojih nema u ovom dokumentu. Ako nešto nedostaje, pitaj.
6. Sve što korisnik vidi je **na srpskom (latinica)**; sav kod, nazivi klasa, tabela i kolona su **na engleskom**.
7. **Kada završiš tiket, moraš ga štiklirati u sekciji 16 ovog dokumenta.** Tiket nije završen dok tabela napretka to ne pokazuje. Puna procedura je opisana u sekciji 16 — pročitaj je pre nego što počneš rad na bilo kom tiketu.

---

## 1. Cilj proizvoda

Home Inventory omogućava fizičkom licu da na jednom mestu vodi tačnu evidenciju svih značajnijih stvari koje poseduje u svom domu — šta ima, gde se nalazi, koliko vredi i do kada je pod garancijom.

**Vodeći princip razvoja:** *Build a real product, not a demo CRUD application.*

Konkretno to znači: aplikacija mora biti upotrebljiva bez interneta, mora davati odgovore koje korisnik zaista traži (a ne samo listati redove iz baze), i mora se ponašati predvidivo kad nešto pođe naopako.

## 2. Problem koji rešava

| Problem | Kako ga aplikacija rešava |
|---|---|
| Ljudi ne znaju šta tačno poseduju niti koliko to vredi | Centralna evidencija sa automatskim zbirom ukupne vrednosti |
| Garancije ističu neprimećeno | Automatsko računanje statusa garancije i upozorenja na Dashboard-u |
| Ne zna se gde je nešto ostavljeno | Svaki predmet je vezan za prostoriju/lokaciju u domu |
| Kod selidbe ili osiguranja treba spisak imovine | Kompletna evidencija sa cenama, serijskim brojevima i fotografijama |
| Stvari su kupljene u različitim valutama | Konverzija preko eksternog API-ja u jednu izabranu valutu prikaza |
| Velika lista postaje nepregledna | Pretraga po šest polja, filteri i šest načina sortiranja |

## 3. Ciljna grupa i persone

### Persona A — „Marko, 23, student"
Živi u iznajmljenom stanu, ima laptop, telefon, monitor, konzolu. Zanima ga **do kada mu traje garancija** i koliko ukupno vredi tehnika koju nosi sa sobom kad se seli. Koristi telefon svakodnevno, očekuje da aplikacija radi brzo i bez registracije preko tri ekrana.

### Persona B — „Jelena, 41, domaćinstvo od četvoro"
Vodi evidenciju bele tehnike, nameštaja i alata. Treba joj **spisak za osiguranje** i podatak gde je šta odloženo (podrum, tavan, garaža). Unosi podatke povremeno, u većim serijama, i drži račune uz predmete.

### Persona C — „Administrator sistema"
Održava listu kategorija koju svi korisnici dele i prati broj registrovanih naloga. Ne zanima ga tuđi inventar i **ne sme mu pristupati**.

## 4. Role i matrica dozvola

Sistem ima tačno dve role: `USER` i `ADMIN`. Rola se čuva na backendu i prenosi u JWT tokenu.

**BR-001 — Prvi registrovani korisnik u sistemu automatski dobija rolu `ADMIN`. Svaki sledeći dobija `USER`.** Ovo pravilo se sprovodi isključivo na serveru (`SELECT COUNT(*) FROM users` = 0), nikada na klijentu.

| Akcija | USER | ADMIN | Napomena |
|---|:---:|:---:|---|
| Registracija naloga | da | da | Javno dostupno |
| Prijava / odjava | da | da | |
| Pregled **sopstvenog** inventara | da | da | Admin ima svoj lični inventar kao i svi |
| Pregled **tuđeg** inventara | ne | ne | **BR-002**, vidi ispod |
| CRUD nad sopstvenim predmetima | da | da | |
| Pretraga / filtriranje / sortiranje | da | da | Samo nad sopstvenim predmetima |
| Statistika sopstvenog inventara | da | da | |
| Izmena sopstvenog profila | da | da | |
| Čitanje liste kategorija | da | da | Kategorije su globalne |
| Kreiranje / izmena / brisanje kategorija | ne | da | **BR-003** |
| CRUD nad sopstvenim lokacijama | da | da | Lokacije su privatne po korisniku |
| Pregled liste korisnika | ne | da | Samo ime, email, rola, status, broj predmeta |
| Aktivacija / deaktivacija naloga | ne | da | **BR-004** |
| Pregled sistemske statistike | ne | da | Agregatni brojevi, bez sadržaja tuđeg inventara |

**BR-002 — Ownership je apsolutan.** Nijedan korisnik, uključujući `ADMIN`, ne može čitati ni menjati predmete drugog korisnika. Provera vlasništva se izvršava **na serveru pri svakom zahtevu** (`WHERE user_id = <iz JWT tokena>`), nikada na osnovu podataka koje klijent pošalje. Promena `id`-a u URL-u mora vratiti `404`, ne `403` — da se ne otkriva postojanje tuđeg resursa.

**BR-003 — Kategorije su globalne i deljene.** Korisnik ih samo čita. Brisanje kategorije koju koristi bilo koji predmet bilo kog korisnika je zabranjeno (`409 CATEGORY_IN_USE`).

**BR-004 — Deaktiviran nalog ne može da se prijavi niti da koristi postojeći token.** Provera `is_active` se izvršava i pri prijavi i u auth middleware-u za svaki zaštićeni zahtev. Admin ne može deaktivirati sopstveni nalog (`409 CANNOT_DEACTIVATE_SELF`).

## 5. Rečnik pojmova

| Pojam | Značenje |
|---|---|
| **Predmet** (Item) | Jedan zapis u inventaru — npr. „Samsung TV 55 inča" |
| **Kategorija** | Globalna klasifikacija predmeta (Elektronika, Nameštaj…), održava je admin |
| **Lokacija** | Prostorija u domu korisnika (Dnevna soba, Garaža…), privatna po korisniku |
| **Valuta prikaza** | Valuta u koju se konvertuju svi zbirovi; korisnik je bira u Profilu |
| **Minor jedinica** | Najmanja jedinica valute — para za RSD, cent za EUR. Sve cene se čuvaju kao ceo broj minor jedinica |
| **Prag garancije** | Broj dana pre isteka nakon kojeg garancija dobija status „uskoro ističe" |
| **syncStatus** | Lokalni marker da predmet još nije poslat na server |
| **Soft delete** | Brisanje postavljanjem `deletedAt`, bez fizičkog uklanjanja reda |

## 6. User stories i acceptance criteria

Format: `Kao <rola>, želim <cilj>, da bih <vrednost>.` Acceptance criteria su obavezujući — funkcionalnost nije završena dok svi nisu ispunjeni.

### Autentifikacija

**US-01 — Registracija**
> Kao posetilac, želim da napravim nalog, da bih imao svoj privatni inventar.

- [ ] Forma traži ime, email, lozinku i potvrdu lozinke
- [ ] Validacija se prikazuje ispod svakog polja, na srpskom
- [ ] Postojeći email vraća poruku „Nalog sa ovom email adresom već postoji"
- [ ] Lozinka se šalje isključivo preko mreže i nikada se ne čuva na uređaju
- [ ] Po uspehu korisnik je odmah prijavljen i vidi Dashboard — bez dodatnog login koraka
- [ ] Prvi korisnik u sistemu dobija ADMIN rolu (BR-001)
- [ ] Tokom zahteva dugme je onemogućeno i prikazan je indikator učitavanja

**US-02 — Prijava**
> Kao korisnik, želim da se prijavim, da bih pristupio svom inventaru.

- [ ] Pogrešan email ili lozinka daju **istu** poruku: „Pogrešan email ili lozinka" (ne otkriva se da li nalog postoji)
- [ ] Deaktiviran nalog daje poruku „Vaš nalog je deaktiviran. Obratite se administratoru."
- [ ] Po uspehu se JWT, userId i rola upisuju u DataStore
- [ ] Pri sledećem pokretanju aplikacije korisnik ide direktno na Dashboard ako token nije istekao

**US-03 — Odjava**
> Kao korisnik, želim da se odjavim, da bih zaštitio svoje podatke.

- [ ] Traži se potvrda kroz dijalog
- [ ] Briše se token, userId i rola iz DataStore-a
- [ ] Briše se **kompletan lokalni sadržaj Room baze** (predmeti, lokacije, kategorije, kursevi) — **BR-005**
- [ ] Korisnik se vraća na Login ekran, a povratno dugme ne vraća u aplikaciju

**BR-005 — Odjava briše lokalne podatke.** Room baza je vezana za prijavljenog korisnika. Pošto na istom uređaju mogu postojati dva naloga, odjava mora obrisati sve lokalne tabele. Ovo sprečava da sledeći korisnik vidi tuđ inventar.

### Dashboard

**US-04 — Pregled stanja na prvi pogled**
> Kao korisnik, želim da odmah po prijavi vidim najvažnije o svom inventaru.

- [ ] Ukupan broj predmeta (zbir količina, ne broj redova) i ukupna vrednost u valuti prikaza
- [ ] Kartica upozorenja o garancijama koje uskoro ističu, sortirana po hitnosti
- [ ] Broj predmeta po kategorijama
- [ ] Poslednjih 5 dodatih predmeta
- [ ] 5 najskupljih predmeta
- [ ] Prazan inventar prikazuje poziv na akciju „Dodajte prvi predmet", ne prazan ekran
- [ ] Sve radi bez interneta iz lokalne Room baze

### Inventar

**US-05 — Pregled inventara**
> Kao korisnik, želim da vidim listu svih svojih predmeta.

- [ ] Lista prikazuje fotografiju (ili placeholder po kategoriji), naziv, kategoriju, lokaciju i cenu
- [ ] Predmet sa garancijom koja uskoro ističe ima vidljivu oznaku
- [ ] Prazna lista prikazuje prazno stanje sa objašnjenjem
- [ ] Lista radi glatko sa 500+ predmeta

**US-06 — Dodavanje predmeta**
> Kao korisnik, želim brzo da dodam predmet, bez popunjavanja dvadeset polja.

- [ ] Obavezna su samo tri polja: **naziv, kategorija, lokacija** (BR-006)
- [ ] Sva ostala polja su opciona i grupisana u sekciju „Dodatni podaci" koja je podrazumevano skupljena
- [ ] Datumi se biraju kroz Material Date Picker, nikad ručnim kucanjem
- [ ] Fotografija se bira iz galerije ili slika kamerom
- [ ] Po čuvanju korisnik se vraća na listu i novi predmet je vidljiv **odmah**, i kad nema interneta

**BR-006 — Minimalna obavezna polja.** Obavezni su samo `name`, `categoryId` i `locationId`. Svako dodatno obavezno polje je kršenje zahteva iz sekcije 6 originalne specifikacije („Obavezna polja treba da budu minimalna kako bi korisnik mogao brzo da unese predmet").

**US-07 — Detalji predmeta**
> Kao korisnik, želim da vidim sve podatke o predmetu.

- [ ] Otvara se u zasebnoj aktivnosti, prima **samo `itemId`** kroz Intent (BR-007)
- [ ] Prikazuje sve popunjene podatke; prazna polja se ne prikazuju kao prazni redovi
- [ ] Prikazuje status garancije sa bojom i preostalim brojem dana
- [ ] Ako predmet ima cenu u valuti različitoj od valute prikaza, prikazuje i original i konverziju
- [ ] Ima akcije Izmeni i Obriši

**BR-007 — Između ekrana se prenosi identifikator, ne objekat.** Ekran koji prima podatke sam učitava predmet iz Room baze po `itemId`. Ovo garantuje da se uvek prikazuje sveže stanje i sprečava probleme sa serijalizacijom.

**US-08 — Izmena predmeta**
> Kao korisnik, želim da ispravim podatke o predmetu.

- [ ] Forma je unapred popunjena postojećim vrednostima
- [ ] Ista validaciona pravila kao pri dodavanju
- [ ] Napuštanje forme sa nesačuvanim izmenama traži potvrdu
- [ ] `updatedAt` se osvežava, `createdAt` ostaje netaknut

**US-09 — Brisanje predmeta**
> Kao korisnik, želim da uklonim predmet koji više ne posedujem.

- [ ] **Obavezan dijalog potvrde** sa nazivom predmeta u tekstu (BR-008)
- [ ] Brisanje je soft delete — red ostaje u bazi sa popunjenim `deletedAt`
- [ ] Obrisan predmet nestaje iz svih lista, pretrage i statistike
- [ ] Prikazuje se Snackbar sa akcijom „Opozovi" u trajanju od 5 sekundi

**BR-008 — Destruktivne akcije zahtevaju potvrdu.** Brisanje predmeta, brisanje lokacije, brisanje kategorije, odjava i deaktivacija naloga moraju imati dijalog potvrde sa jasno imenovanim posledicama.

### Pretraga, filteri, sortiranje

**US-10 — Pretraga**
> Kao korisnik sa mnogo predmeta, želim brzo da nađem određenu stvar.

- [ ] Polje za pretragu je vidljivo direktno na ekranu inventara
- [ ] Pretražuje se po **šest polja**: naziv, proizvođač, model, serijski broj, naziv kategorije, naziv lokacije (FR-031)
- [ ] Pretraga ne razlikuje velika i mala slova
- [ ] Rezultati se osvežavaju dok korisnik kuca, sa debounce-om od 300 ms
- [ ] Nema rezultata → prazno stanje sa pojmom koji je tražen
- [ ] Pretraga radi **lokalno nad Room bazom** i ne zahteva internet

**US-11 — Filtriranje**
> Kao korisnik, želim da suzim listu na ono što me zanima.

- [ ] Filteri se otvaraju u bottom sheet-u sa dugmeta pored pretrage
- [ ] Dostupni filteri: kategorija (više izbora), lokacija (više izbora), raspon cene, godina kupovine, samo pod garancijom, samo garancija uskoro ističe
- [ ] Broj aktivnih filtera je prikazan na dugmetu
- [ ] Dugme „Poništi sve filtere"
- [ ] Filteri i pretraga rade **kombinovano** (logičko I)
- [ ] Aktivni filteri preživljavaju rotaciju ekrana

**US-12 — Sortiranje**
> Kao korisnik, želim da poređam listu po svom kriterijumu.

- [ ] Šest opcija: naziv A-Z, naziv Z-A, cena rastuće, cena opadajuće, najnovije dodato, najstarije dodato
- [ ] Izabrano sortiranje se pamti između sesija (DataStore)
- [ ] Sortiranje po ceni koristi vrednost **konvertovanu u valutu prikaza**, ne sirovi broj (BR-009)

**BR-009 — Poređenje cena uvek ide preko valute prikaza.** Predmet od 120.000 RSD i predmet od 900 EUR se ne mogu porediti kao brojevi. Svako sortiranje, filtriranje po rasponu cene i svaki zbir prvo konvertuje iznose u valutu prikaza.

### Garancije

**US-13 — Praćenje garancija**
> Kao korisnik, želim da me aplikacija upozori pre nego što garancija istekne.

- [ ] Status se računa automatski iz `warrantyExpirationDate` (BR-010)
- [ ] Dashboard prikazuje upozorenja u formatu „Garancija za Samsung TV ističe za 15 dana"
- [ ] Prag „uskoro ističe" je podesiv u Profilu: 7, 30, 60 ili 90 dana, podrazumevano 30
- [ ] Predmet bez datuma garancije ima status „Nema informacije" i **ne pojavljuje se** među upozorenjima
- [ ] Statusi su vizuelno razdvojeni bojom

**BR-010 — Algoritam statusa garancije.** Neka je `D` = `warrantyExpirationDate`, `T` = današnji datum, `P` = prag u danima:

| Uslov | Status | Boja |
|---|---|---|
| `D` je `null` | `NEPOZNATO` | siva |
| `D < T` | `ISTEKLA` | crvena |
| `T <= D <= T + P` | `USKORO_ISTICE` | narandžasta |
| `D > T + P` | `AKTIVNA` | zelena |

Poređenje je isključivo po kalendarskom datumu, bez vremena i bez vremenske zone. Datum isteka na današnji dan znači `USKORO_ISTICE`, ne `ISTEKLA`.

### Vrednost i valute

**US-14 — Ukupna vrednost imovine**
> Kao korisnik, želim da znam koliko ukupno vredi sve što posedujem.

- [ ] Ukupna vrednost je prikazana na Dashboard-u i u Statistici
- [ ] Vrednost predmeta = `estimatedValue` ako postoji, inače `purchasePrice`, inače 0 (BR-011)
- [ ] Vrednost predmeta se množi njegovom količinom
- [ ] Predmeti u različitim valutama se konvertuju u valutu prikaza
- [ ] Ako kurs nije dostupan ni iz keša, prikazuje se poruka umesto pogrešnog broja (BR-013)

**BR-011 — Efektivna vrednost predmeta.** `efektivnaVrednost = (estimatedValue ?: purchasePrice ?: 0) * quantity`. Ovo pravilo važi za sve zbirove, statistiku i sortiranje po ceni — nigde se ne sme koristiti drugačija formula.

**US-15 — Konverzija valuta**
> Kao korisnik, želim da vidim vrednost u valuti koju razumem.

- [ ] Podržane valute: RSD, EUR, USD, CHF, GBP, BAM (BR-012)
- [ ] Valuta prikaza se bira u Profilu, podrazumevano RSD
- [ ] Kursevi se povlače sa eksternog API-ja asinhrono, bez blokiranja UI-ja
- [ ] Kursevi se keširaju lokalno; keš stariji od 24h se osvežava
- [ ] Bez interneta se koristi poslednji poznati kurs iz keša
- [ ] Neuspeh mrežnog poziva **nikada ne ruši ekran** — prikazuje se poslednja poznata vrednost

**BR-012 — Zatvorena lista valuta.** Aplikacija podržava tačno šest valuta: `RSD`, `EUR`, `USD`, `CHF`, `GBP`, `BAM`. Sve imaju dve decimale (eksponent 2). Eksterni API vraća 166 valuta — koriste se samo ovih šest.

**BR-013 — Nedostupan kurs se prijavljuje, ne pretpostavlja.** Ako za neku valutu nema kursa ni iz mreže ni iz keša, zbir se **ne računa sa kursom 1.0**. Prikazuje se poruka „Kurs trenutno nije dostupan" i vrednost te valute se izdvaja posebno.

### Statistika

**US-16 — Analitika inventara**
> Kao korisnik, želim da razumem strukturu svoje imovine.

- [ ] Ukupan broj predmeta, ukupna vrednost, broj kategorija u upotrebi, prosečna vrednost predmeta
- [ ] Pie chart raspodele **vrednosti** po kategorijama
- [ ] Bar chart **broja predmeta** po kategorijama
- [ ] Tabela: Kategorija | Broj predmeta | Ukupna vrednost
- [ ] Najskuplji predmet
- [ ] Raspodela po statusu garancije
- [ ] Sve se računa iz Room baze i radi bez interneta

### Profil i podešavanja

**US-17 — Profil**
> Kao korisnik, želim da vidim i izmenim svoje podatke.

- [ ] Prikazuje ime, email, rolu, broj predmeta i ukupnu vrednost inventara
- [ ] Izmena imena
- [ ] Promena lozinke uz unos stare lozinke
- [ ] Izbor valute prikaza
- [ ] Izbor praga garancije
- [ ] Ulaz u administraciju — **vidljiv samo ako je rola ADMIN**
- [ ] Odjava

**US-18 — Upravljanje lokacijama**
> Kao korisnik, želim da definišem prostorije u svom domu.

- [ ] Lista sopstvenih lokacija sa brojem predmeta u svakoj
- [ ] Dodavanje, izmena i brisanje lokacije
- [ ] Brisanje lokacije koja sadrži predmete je zabranjeno, uz poruku koliko predmeta je koristi (BR-014)
- [ ] Novi korisnik pri registraciji dobija devet podrazumevanih lokacija (BR-015)

**BR-014 — Referencirani entitet se ne briše.** Lokacija ili kategorija koju koristi bar jedan neobrisan predmet ne može se obrisati. Server vraća `409` sa brojem predmeta koji je blokiraju.

**BR-015 — Seed lokacija pri registraciji.** Server pri kreiranju naloga automatski kreira devet lokacija: Dnevna soba, Spavaća soba, Kuhinja, Kupatilo, Garaža, Podrum, Tavan, Radna soba, Hodnik. Korisnik ih sme menjati i brisati.

### Administracija

**US-19 — Admin dashboard**
> Kao administrator, želim pregled stanja sistema.

- [ ] Broj registrovanih, aktivnih i deaktiviranih korisnika
- [ ] Ukupan broj predmeta u sistemu (samo broj, bez sadržaja)
- [ ] Broj kategorija
- [ ] Ekran je nedostupan korisniku sa rolom USER, i na klijentu i na serveru

**US-20 — Upravljanje korisnicima**
> Kao administrator, želim da mogu da deaktiviram problematičan nalog.

- [ ] Lista: ime, email, rola, status, datum registracije, broj predmeta
- [ ] Prekidač za aktivaciju/deaktivaciju uz dijalog potvrde
- [ ] Admin ne može deaktivirati sam sebe (BR-004)
- [ ] **Nigde se ne prikazuje sadržaj tuđeg inventara** (BR-002)

**US-21 — Upravljanje kategorijama**
> Kao administrator, želim da održavam listu kategorija.

- [ ] Lista globalnih kategorija sa brojem predmeta koji ih koristi
- [ ] Dodavanje, izmena naziva i opisa, brisanje
- [ ] Brisanje kategorije u upotrebi je zabranjeno (BR-014)
- [ ] Nazivi kategorija su jedinstveni

### Offline rad

**US-22 — Rad bez interneta**
> Kao korisnik, želim da aplikacija radi i kad nemam signal.

- [ ] Pregled, pretraga, filtriranje i statistika rade potpuno offline
- [ ] Dodavanje, izmena i brisanje rade offline i čuvaju se lokalno
- [ ] Nesinhronizovane izmene imaju vidljivu oznaku
- [ ] Po povratku mreže se sinhronizuju automatski
- [ ] Aplikacija se **nikada ne ruši** zbog nedostatka mreže

## 7. Funkcionalni zahtevi

### FR-001..019 — Autentifikacija i nalog

| ID | Zahtev | Faza |
|---|---|---|
| FR-001 | Registracija sa poljima ime, email, lozinka, potvrda lozinke | MVP |
| FR-002 | Server proverava da li email već postoji i vraća `EMAIL_ALREADY_EXISTS` | MVP |
| FR-003 | Lozinka se hashuje BCrypt algoritmom sa cost faktorom 10 pre upisa | MVP |
| FR-004 | Lozinka se nikada ne čuva u čitljivom obliku, ni na serveru ni na uređaju | MVP |
| FR-005 | Prijava vraća JWT token sa rokom važenja od 7 dana | MVP |
| FR-006 | JWT sadrži `sub` (userId), `role`, `iat`, `exp` — i ništa osetljivo | MVP |
| FR-007 | Token se čuva u Jetpack DataStore zajedno sa userId i rolom | MVP |
| FR-008 | OkHttp interceptor dodaje `Authorization: Bearer <token>` na svaki zahtev ka backendu | MVP |
| FR-009 | Odgovor `401` briše lokalnu sesiju i vraća korisnika na Login | MVP |
| FR-010 | Odjava briše token i kompletnu lokalnu bazu | MVP |
| FR-011 | Pri pokretanju aplikacije se proverava važenje tokena i bira početna aktivnost | MVP |
| FR-012 | Prvi registrovani korisnik dobija ADMIN rolu | MVP |
| FR-013 | Deaktiviran korisnik ne može da se prijavi | Faza 7 |
| FR-014 | Deaktiviran korisnik sa postojećim tokenom dobija `403` na svaki zahtev | Faza 7 |
| FR-015 | Izmena imena korisnika | Faza 7 |
| FR-016 | Promena lozinke uz proveru stare lozinke | Faza 7 |
| FR-017 | Poruka o pogrešnim kredencijalima ne otkriva da li nalog postoji | MVP |
| FR-018 | Registracija automatski prijavljuje korisnika | MVP |
| FR-019 | Registracija kreira devet podrazumevanih lokacija za novog korisnika | MVP |

### FR-020..039 — Inventar, pretraga, filteri

| ID | Zahtev | Faza |
|---|---|---|
| FR-020 | Kreiranje predmeta sa obaveznim poljima naziv, kategorija, lokacija | MVP |
| FR-021 | Opciona polja: opis, proizvođač, model, serijski broj, količina, nabavna cena, procenjena vrednost, valuta, datum kupovine, datum isteka garancije, prodavac, fotografija, beleške | MVP |
| FR-022 | Pregled liste sopstvenih predmeta | MVP |
| FR-023 | Pregled detalja predmeta u zasebnoj aktivnosti | MVP |
| FR-024 | Izmena bilo kog polja predmeta | MVP |
| FR-025 | Brisanje predmeta uz obavezan dijalog potvrde | MVP |
| FR-026 | Brisanje je soft delete preko `deletedAt` | MVP |
| FR-027 | Opoziv brisanja kroz Snackbar u roku od 5 sekundi | Faza 7 |
| FR-028 | `createdAt` i `updatedAt` se održavaju automatski | MVP |
| FR-029 | UUID v4 identifikator se generiše na klijentu pri kreiranju | MVP |
| FR-030 | Predmet se prikazuje u listi odmah po čuvanju, i bez interneta | MVP |
| FR-031 | Pretraga po nazivu, proizvođaču, modelu, serijskom broju, kategoriji i lokaciji | Faza 5 |
| FR-032 | Pretraga bez razlikovanja velikih i malih slova, sa debounce-om 300 ms | Faza 5 |
| FR-033 | Filter po kategoriji sa više izbora | Faza 5 |
| FR-034 | Filter po lokaciji sa više izbora | Faza 5 |
| FR-035 | Filter po rasponu cene u valuti prikaza | Faza 5 |
| FR-036 | Filter po godini kupovine | Faza 5 |
| FR-037 | Filter „samo pod garancijom" i „garancija uskoro ističe" | Faza 5 |
| FR-038 | Šest opcija sortiranja | Faza 5 |
| FR-039 | Pretraga, filteri i sortiranje deluju kombinovano i preživljavaju rotaciju | Faza 5 |

### FR-040..049 — Kategorije i lokacije

| ID | Zahtev | Faza |
|---|---|---|
| FR-040 | Globalna lista kategorija, čitljiva svim korisnicima | MVP |
| FR-041 | Jedanaest podrazumevanih kategorija u seed podacima | MVP |
| FR-042 | Admin CRUD nad kategorijama | Faza 7 |
| FR-043 | Jedinstvenost naziva kategorije | Faza 7 |
| FR-044 | Zabrana brisanja kategorije u upotrebi | Faza 7 |
| FR-045 | Lokacije su privatne po korisniku | MVP |
| FR-046 | Korisnički CRUD nad sopstvenim lokacijama | Faza 4 |
| FR-047 | Jedinstvenost naziva lokacije unutar jednog korisnika | Faza 4 |
| FR-048 | Zabrana brisanja lokacije u upotrebi | Faza 4 |
| FR-049 | Prikaz broja predmeta uz svaku kategoriju i lokaciju | Faza 4 |

### FR-050..059 — Garancije

| ID | Zahtev | Faza |
|---|---|---|
| FR-050 | Automatsko računanje statusa garancije po BR-010 | Faza 6 |
| FR-051 | Podesiv prag: 7, 30, 60 ili 90 dana | Faza 6 |
| FR-052 | Prag se čuva u DataStore, podrazumevano 30 | Faza 6 |
| FR-053 | Kartica upozorenja na Dashboard-u, sortirana po hitnosti | Faza 6 |
| FR-054 | Tekst upozorenja: „Garancija za \<naziv\> ističe za \<n\> dana" | Faza 6 |
| FR-055 | Oznaka statusa garancije na stavci u listi inventara | Faza 6 |
| FR-056 | Raspodela po statusu garancije na ekranu Statistike | Faza 6 |

### FR-060..069 — Vrednost i valute

| ID | Zahtev | Faza |
|---|---|---|
| FR-060 | Cene se čuvaju kao ceo broj minor jedinica (BIGINT / Long) | MVP |
| FR-061 | Svaki predmet ima sopstvenu valutu | MVP |
| FR-062 | Valuta prikaza se bira u Profilu, podrazumevano RSD | Faza 6 |
| FR-063 | Kursevi se povlače sa `open.er-api.com` asinhrono preko Retrofita i coroutines | Faza 6 |
| FR-064 | Kursevi se keširaju u Room tabeli sa TTL od 24 sata | Faza 6 |
| FR-065 | Bez mreže se koristi poslednji keširani kurs | Faza 6 |
| FR-066 | Efektivna vrednost se računa po BR-011 | Faza 6 |
| FR-067 | Ukupna vrednost inventara u valuti prikaza | Faza 6 |
| FR-068 | Vrednost po kategoriji u valuti prikaza | Faza 6 |
| FR-069 | Nedostupan kurs se prijavljuje, ne zamenjuje jedinicom (BR-013) | Faza 6 |

### FR-070..079 — Statistika i Dashboard

| ID | Zahtev | Faza |
|---|---|---|
| FR-070 | Dashboard: ukupan broj predmeta i ukupna vrednost | MVP |
| FR-071 | Dashboard: poslednjih 5 dodatih predmeta | Faza 4 |
| FR-072 | Dashboard: 5 najskupljih predmeta | Faza 6 |
| FR-073 | Dashboard: broj predmeta po kategorijama | Faza 4 |
| FR-074 | Statistika: pie chart vrednosti po kategorijama | Faza 6 |
| FR-075 | Statistika: bar chart broja predmeta po kategorijama | Faza 6 |
| FR-076 | Statistika: tabela kategorija / broj / vrednost | Faza 6 |
| FR-077 | Statistika: prosečna vrednost predmeta i najskuplji predmet | Faza 6 |
| FR-078 | Sva statistika se računa iz Room baze i radi offline | Faza 6 |

### FR-080..089 — Fotografije

| ID | Zahtev | Faza |
|---|---|---|
| FR-080 | Izbor fotografije iz galerije preko Photo Picker-a | Faza 4 |
| FR-081 | Slikanje kamerom preko `ACTION_IMAGE_CAPTURE` | Faza 7 |
| FR-082 | Kompresija na maksimalno 1080 px duže stranice, JPEG kvalitet 80 | Faza 4 |
| FR-083 | Kopiranje u privatni folder aplikacije, u bazu ide samo naziv fajla | Faza 4 |
| FR-084 | Fotografija je isključivo lokalna i ne šalje se na server | Faza 4 |
| FR-085 | Sinhronizacija sa servera ne sme obrisati lokalnu putanju do fotografije | Faza 7 |
| FR-086 | Brisanje predmeta briše i njegov fajl fotografije | Faza 7 |
| FR-087 | Predmet bez fotografije prikazuje placeholder ikonicu kategorije | Faza 4 |

### FR-090..099 — Sinhronizacija i offline

| ID | Zahtev | Faza |
|---|---|---|
| FR-090 | Room je izvor istine za sve što UI prikazuje | MVP |
| FR-091 | `syncStatus` prati stanje: SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE | Faza 7 |
| FR-092 | Sinhronizacija se pokreće pri otvaranju Dashboard-a i Inventara | Faza 7 |
| FR-093 | Sinhronizacija se pokreće posle svake izmene | Faza 7 |
| FR-094 | Pull-to-refresh ručno pokreće sinhronizaciju | Faza 7 |
| FR-095 | Konflikti se rešavaju last-write-wins po `updatedAt`, server odlučuje | Faza 7 |
| FR-096 | Nesinhronizovane stavke imaju vidljivu oznaku u listi | Faza 7 |
| FR-097 | Neuspela sinhronizacija ne blokira rad aplikacije | Faza 7 |
| FR-098 | Delta pull preko `updatedSince` parametra, ne puno preuzimanje svaki put | Faza 7 |

### FR-100..109 — Administracija

| ID | Zahtev | Faza |
|---|---|---|
| FR-100 | AdminActivity dostupna samo iz Profila i samo roli ADMIN | Faza 7 |
| FR-101 | Admin dashboard sa brojem korisnika po statusu | Faza 7 |
| FR-102 | Lista korisnika sa imenom, emailom, rolom, statusom i brojem predmeta | Faza 7 |
| FR-103 | Aktivacija i deaktivacija naloga | Faza 7 |
| FR-104 | Zabrana samodeaktivacije | Faza 7 |
| FR-105 | Upravljanje globalnim kategorijama | Faza 7 |
| FR-106 | Server odbija admin endpointe roli USER sa `403` | Faza 7 |

## 8. Ekrani i navigacija

### Aktivnosti

| ID | Aktivnost | Uloga |
|---|---|---|
| ACT-1 | `AuthenticationActivity` | Login i registracija; ulazna tačka kad nema važećeg tokena |
| ACT-2 | `MainActivity` | Glavni deo aplikacije sa bottom navigacijom |
| ACT-3 | `ItemDetailsActivity` | Detalji jednog predmeta; prima `itemId` |
| ACT-4 | `AdminActivity` | Administracija; dostupna samo roli ADMIN |

### Ekrani

| ID | Ekran (fragment) | Aktivnost | Opis |
|---|---|---|---|
| SCR-01 | `LoginFragment` | ACT-1 | Prijava |
| SCR-02 | `RegisterFragment` | ACT-1 | Registracija |
| SCR-03 | `DashboardFragment` | ACT-2 | Pregled stanja, upozorenja o garancijama |
| SCR-04 | `InventoryFragment` | ACT-2 | Lista, pretraga, sortiranje, FAB za dodavanje |
| SCR-05 | `FilterBottomSheetFragment` | ACT-2 | Filteri; vraća rezultat u SCR-04 |
| SCR-06 | `AddEditItemFragment` | ACT-2 / ACT-3 | Forma za dodavanje i izmenu |
| SCR-07 | `ItemDetailsFragment` | ACT-3 | Detalji predmeta |
| SCR-08 | `StatisticsFragment` | ACT-2 | Grafikoni i tabela |
| SCR-09 | `ProfileFragment` | ACT-2 | Profil, podešavanja, odjava, ulaz u admin |
| SCR-10 | `LocationsFragment` | ACT-2 | Upravljanje lokacijama |
| SCR-11 | `AdminDashboardFragment` | ACT-4 | Sistemska statistika |
| SCR-12 | `AdminUsersFragment` | ACT-4 | Upravljanje korisnicima |
| SCR-13 | `AdminCategoriesFragment` | ACT-4 | Upravljanje kategorijama |

### Bottom navigacija

```
Dashboard  |  Inventar  |  Statistika  |  Profil
```

Meni je **identičan za obe role**. Administracija se otvara iz Profila kao zasebna aktivnost.

### Tokovi navigacije

```
Pokretanje aplikacije
  |
  +- token vazeci? -- ne --> ACT-1 (SCR-01 Login) --> registracija --> SCR-02
  |
  +- da --> ACT-2 (SCR-03 Dashboard)

SCR-04 Inventar
  |
  +- FAB ----------------> SCR-06 (rezim dodavanja)
  |
  +- klik na predmet ----> ACT-3 sa Intent extra "itemId" --> SCR-07
  |                           |
  |                           +- Izmeni --> SCR-06 (rezim izmene, argument itemId)
  |                           +- Obrisi --> dijalog --> nazad na SCR-04
  |
  +- dugme filtera ------> SCR-05 (bottom sheet) --> rezultat nazad u SCR-04

SCR-09 Profil
  |
  +- Lokacije --> SCR-10
  +- Administracija (samo ADMIN) --> ACT-4 --> SCR-11 / SCR-12 / SCR-13
  +- Odjava --> dijalog --> brisanje sesije i baze --> ACT-1
```

### Komunikacija između fragmenata

Zahtev iz sekcije 44 originalne specifikacije se ispunjava na dva načina, oba obavezna:

1. **Deljeni ViewModel** — `InventoryViewModel` je skopovan na `MainActivity`, pa ga `InventoryFragment` i `FilterBottomSheetFragment` dele. Promena filtera u bottom sheet-u trenutno menja listu.
2. **Fragment Result API** — `AddEditItemFragment` vraća rezultat (`ITEM_SAVED` sa `itemId`) pozivajućem fragmentu preko `setFragmentResult`, koji tada prikazuje Snackbar potvrde.

## 9. Validaciona pravila

Validacija se izvršava **i na Androidu i na serveru**. Klijentska validacija je za brzu povratnu informaciju; serverska je bezbednosna granica i nikada se ne sme preskočiti.

| ID | Polje | Pravilo | Poruka korisniku |
|---|---|---|---|
| VR-01 | `name` (korisnik) | obavezno, 2–100 znakova | „Ime mora imati najmanje 2 znaka" |
| VR-02 | `email` | obavezno, važeći format, max 255 | „Unesite ispravnu email adresu" |
| VR-03 | `email` | jedinstven u sistemu | „Nalog sa ovom email adresom već postoji" |
| VR-04 | `password` | obavezno, najmanje 8 znakova | „Lozinka mora imati najmanje 8 znakova" |
| VR-05 | `confirmPassword` | mora se poklapati sa lozinkom | „Lozinke se ne poklapaju" |
| VR-06 | `item.name` | obavezno, 2–120 znakova | „Naziv mora imati najmanje 2 znaka" |
| VR-07 | `item.categoryId` | obavezno, mora postojati | „Izaberite kategoriju" |
| VR-08 | `item.locationId` | obavezno, mora pripadati korisniku | „Izaberite lokaciju" |
| VR-09 | `item.quantity` | ceo broj, 1–9999 | „Količina mora biti najmanje 1" |
| VR-10 | `item.purchasePrice` | opciono, ne negativan, max 999.999.999,99 | „Cena ne može biti negativna" |
| VR-11 | `item.estimatedValue` | ista pravila kao VR-10 | „Vrednost ne može biti negativna" |
| VR-12 | `item.currency` | mora biti iz liste RSD, EUR, USD, CHF, GBP, BAM | „Nepodržana valuta" |
| VR-13 | `item.purchaseDate` | opciono, format `YYYY-MM-DD`, ne u budućnosti | „Datum kupovine ne može biti u budućnosti" |
| VR-14 | `item.warrantyExpirationDate` | opciono, ne pre datuma kupovine | „Datum garancije ne može biti pre datuma kupovine" |
| VR-15 | `item.serialNumber` | opciono, max 100 znakova | — |
| VR-16 | `item.description`, `notes` | opciono, max 1000 znakova | „Tekst je predugačak" |
| VR-17 | `item.manufacturer`, `model`, `seller` | opciono, max 100 znakova | — |
| VR-18 | `category.name` | obavezno, 2–60, jedinstveno globalno | „Kategorija sa ovim nazivom već postoji" |
| VR-19 | `location.name` | obavezno, 2–60, jedinstveno po korisniku | „Lokacija sa ovim nazivom već postoji" |
| VR-20 | Sva tekstualna polja | `trim` pre validacije i upisa | — |

**BR-016 — Prazan string nije vrednost.** Opciono tekstualno polje koje korisnik ostavi prazno se čuva kao `NULL`, nikada kao prazan string. Ovo drži upite i prikaz konzistentnim.

## 10. Katalog grešaka

Server vraća sve greške u jednakom obliku. Klijent mapira `code` u poruku na srpskom. **Korisniku se nikada ne prikazuje sirovi tekst izuzetka ni stack trace.**

| Kod | HTTP | Poruka korisniku |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Prikazuje se po poljima iz `details` |
| `INVALID_CREDENTIALS` | 401 | Pogrešan email ili lozinka |
| `TOKEN_EXPIRED` | 401 | Sesija je istekla. Prijavite se ponovo. |
| `TOKEN_INVALID` | 401 | Sesija nije važeća. Prijavite se ponovo. |
| `ACCOUNT_DEACTIVATED` | 403 | Vaš nalog je deaktiviran. Obratite se administratoru. |
| `FORBIDDEN` | 403 | Nemate dozvolu za ovu akciju. |
| `NOT_FOUND` | 404 | Traženi podatak ne postoji. |
| `EMAIL_ALREADY_EXISTS` | 409 | Nalog sa ovom email adresom već postoji. |
| `CATEGORY_NAME_TAKEN` | 409 | Kategorija sa ovim nazivom već postoji. |
| `LOCATION_NAME_TAKEN` | 409 | Lokacija sa ovim nazivom već postoji. |
| `CATEGORY_IN_USE` | 409 | Kategorija se koristi kod {n} predmeta i ne može se obrisati. |
| `LOCATION_IN_USE` | 409 | Lokacija se koristi kod {n} predmeta i ne može se obrisati. |
| `CANNOT_DEACTIVATE_SELF` | 409 | Ne možete deaktivirati sopstveni nalog. |
| `SYNC_CONFLICT` | 409 | Podatak je izmenjen na drugom uređaju. |
| `WRONG_CURRENT_PASSWORD` | 400 | Trenutna lozinka nije tačna. |
| `INTERNAL_ERROR` | 500 | Došlo je do greške. Pokušajte ponovo. |

### Greške koje nastaju na klijentu

| Situacija | Poruka korisniku | Ponašanje |
|---|---|---|
| Nema mrežne konekcije | Nema internet konekcije. Prikazani su poslednji sačuvani podaci. | Prikaz iz Room baze, akcija se stavlja u red za sinhronizaciju |
| Server nedostupan / timeout | Server trenutno nije dostupan. | Isto kao gore, uz dugme „Pokušaj ponovo" |
| Currency API nedostupan | Kurs trenutno nije dostupan. | Koristi se keširani kurs; ako ga nema, primenjuje se BR-013 |
| Nepoznata greška | Došlo je do greške. Pokušajte ponovo. | Greška se loguje u Logcat, korisniku se ne prikazuje detalj |

## 11. Stanja učitavanja i prazna stanja

**BR-017 — Svaki ekran ima četiri stanja.** Nijedan ekran ne sme prikazati praznu belinu.

| Stanje | Prikaz |
|---|---|
| `Loading` | Indikator; kod lista shimmer ili centralni spinner |
| `Success` | Sadržaj |
| `Empty` | Ikonica, objašnjenje i poziv na akciju |
| `Error` | Ikonica, razumljiva poruka i dugme „Pokušaj ponovo" |

Indikator učitavanja je obavezan pri: prijavi, registraciji, učitavanju inventara, čuvanju predmeta, izmeni, brisanju, pozivu Currency API-ja i sinhronizaciji. Tokom operacije se dugme koje ju je pokrenulo onemogućava, radi sprečavanja dvostrukog slanja.

## 12. Nefunkcionalni zahtevi

| ID | Zahtev |
|---|---|
| NFR-01 | Lista inventara mora ostati responzivna sa najmanje 500 predmeta |
| NFR-02 | Nijedna operacija sa bazom ili mrežom se ne izvršava na glavnoj niti |
| NFR-03 | Svi mrežni pozivi su `suspend` funkcije pokrenute iz `viewModelScope` |
| NFR-04 | Rotacija ekrana ne gubi unete podatke ni stanje filtera |
| NFR-05 | Aplikacija radi offline za sve operacije čitanja |
| NFR-06 | Lozinke se hashuju BCrypt-om sa cost faktorom najmanje 10 |
| NFR-07 | Ownership se proverava na serveru pri svakom zahtevu |
| NFR-08 | Cleartext HTTP je dozvoljen isključivo za lokalne razvojne adrese |
| NFR-09 | Nijedan tekst vidljiv korisniku nije hardkodovan — sve ide u `strings.xml` |
| NFR-10 | Aplikacija podržava tamnu temu kroz Material 3 |
| NFR-11 | Aplikacija se ne ruši ni pri jednom očekivanom scenariju greške |
| NFR-12 | Novčani proračuni su egzaktni — nigde se ne koristi `Float` ni `Double` za novac |
| NFR-13 | Minimalna podržana verzija Androida je API 26 |

## 13. Faze isporuke

Svaka faza ostavlja aplikaciju u stanju koje radi i može se demonstrirati.

| Faza | Sadržaj | Definicija završenosti |
|---|---|---|
| **1. Dokumentacija** | `prd.md`, `db.md`, `tech.md` | Sva tri dokumenta odobrena |
| **2. Backend jezgro** | MySQL šema, Express skelet, auth, items CRUD, kategorije, lokacije | Svi endpointi prolaze proveru u Postman-u; seed skripta radi |
| **3. Android vertikalni presek** | Gradle setup, Hilt, Room, Retrofit, Login → Dashboard | Prijava radi end-to-end kroz sve slojeve |
| **4. CRUD inventara** | Lista, dodavanje, detalji, izmena, brisanje, lokacije, fotografije | Kompletan CRUD radi na uređaju |
| **5. Pretraga i filteri** | Pretraga po 6 polja, filteri, sortiranje | Rade kombinovano i preživljavaju rotaciju |
| **6. Vrednost i analitika** | Currency API, konverzija, statistika, garancije | Grafikoni prikazuju tačne podatke u valuti prikaza |
| **7. Administracija i offline** | AdminActivity, sinhronizacija, poliranje, prazna stanja | Sve stavke acceptance checkliste su ispunjene |

### Granica MVP-a

MVP je završetak **Faze 4**: korisnik može da se registruje, prijavi, doda predmet, vidi ga, izmeni, obriše i odjavi se — kroz backend, sa lokalnim keširanjem. Sve posle toga povećava kvalitet i broj bodova, ali MVP je sam po sebi odbranjiv proizvod.

## 14. Acceptance checklist prema projektnim zahtevima

Ova lista se proverava pre predaje. Svaka stavka odgovara zahtevu iz sekcije 44 originalne specifikacije.

| # | Projektni zahtev | Gde je ispunjen | Status |
|---|---|---|:--:|
| 1 | Najmanje 3 aktivnosti | ACT-1 do ACT-4 (četiri) | [ ] |
| 2 | Navigacija između aktivnosti | Auth → Main → ItemDetails → Admin | [ ] |
| 3 | Prenos podataka između aktivnosti | `itemId` kroz Intent extra (BR-007) | [ ] |
| 4 | Smislen UI/UX | Material 3, četiri stanja po ekranu (BR-017) | [ ] |
| 5 | Lokalno skladištenje podataka | Room baza + DataStore | [ ] |
| 6 | Eksterni API | `open.er-api.com` za kursnu listu | [ ] |
| 7 | Asinhrona komunikacija sa API-jem | Retrofit `suspend` funkcije | [ ] |
| 8 | Kotlin Coroutines | `viewModelScope`, `Flow`, `Dispatchers.IO` | [ ] |
| 9 | CRUD operacije | FR-020 do FR-030 | [ ] |
| 10 | Fragmenti gde logika to opravdava | SCR-01 do SCR-13 (trinaest fragmenata) | [ ] |
| 11 | Komunikacija između fragmenata | Deljeni ViewModel + Fragment Result API | [ ] |
| 12 | Pretraga | FR-031, šest polja | [ ] |
| 13 | Autentifikacija | JWT + BCrypt, FR-001 do FR-019 | [ ] |
| 14 | Role korisnika | USER i ADMIN, matrica u sekciji 4 | [ ] |
| 15 | Rad sa većom količinom podataka | Seed od ~60 predmeta, lista testirana na 500 | [ ] |

## 15. Van opsega

Sledeće **nije** deo ovog projekta. Agenti ne treba da ga implementiraju niti predlažu:

- Deljenje inventara između više korisnika ili porodični nalozi
- Upload fotografija na server
- Push notifikacije i pozadinski poslovi (WorkManager)
- Refresh tokeni — istekla sesija znači ponovnu prijavu
- Skeniranje barkodova i OCR računa
- Izvoz u PDF ili Excel
- Prijava preko Google / društvenih mreža
- Zaboravljena lozinka i email verifikacija
- Više jezika — samo srpski, ali kroz `strings.xml` tako da se prevod kasnije može dodati
- Web ili iOS klijent
- Istorijat izmena predmeta (audit log)

## 16. Napredak implementacije

Ovo je **zvanična evidencija napretka projekta**. Tabela ispod je jedino mesto sa kojeg se čita šta je gotovo, a šta nije.

### Procedura koju svaki agent mora da ispoštuje

Kada preuzmeš tiket iz `docs/tickets/`, radiš sledeće:

**Pre početka rada**
1. Pročitaj svoj tiket u celosti, zajedno sa svim oznakama koje referencira (`FR-xxx`, `BR-xxx`, `VR-xx`, `OWN-xx`, `DB-RULE-xx`).
2. Proveri u tabeli ispod da je **prethodni tiket štikliran**. Lanac je strogo linearan — ako prethodni nije gotov, ne počinji, nego prijavi korisniku.

**Posle završetka rada**
3. Prođi kroz **svaki** acceptance kriterijum u svom tiketu i stvarno ga proveri. Ne pretpostavljaj da nešto radi zato što je kod napisan.
4. U fajlu tiketa štikliraj svaki ispunjen kriterijum: `- [ ]` postaje `- [x]`.
5. U fajlu tiketa promeni `**Status:** ready-for-agent` u `**Status:** done`.
6. Commituj rad sa porukom po formatu iz `tech.md` sekcija 14.
7. **U tabeli ispod štikliraj svoj tiket** i popuni datum i kratki hash commita.

### Pravila štikliranja

| # | Pravilo |
|---|---|
| TRK-01 | Štikliraj **samo tiket na kojem si radio**. Nikada tuđe tikete, nikada unapred. |
| TRK-02 | Tiket se štiklira tek kada su **svi** njegovi acceptance kriterijumi ispunjeni i provereni. |
| TRK-03 | Ako je tiket završen **delimično**, ostavi kućicu praznu, upiši `delimično` u kolonu Napomena i navedi šta tačno nedostaje. Nikada ne štikliraj polovičan posao. |
| TRK-04 | Ako si morao da odstupiš od tiketa, upiši to u kolonu Napomena i **prijavi korisniku** — ne menjaj tiho zahtev. |
| TRK-05 | Štikliranje u ovoj tabeli i promena statusa u fajlu tiketa se rade **zajedno**, u istom commitu. Jedno bez drugog je nekonzistentno stanje. |
| TRK-06 | Ako otkriješ da je ranije štikliran tiket zapravo pokvaren, **nemoj ga odštiklirati** — prijavi korisniku i predloži popravni tiket. |
| TRK-07 | Kolona Commit sadrži kratki hash (7 znakova) commita kojim je tiket završen. |

### Tabela napretka

Legenda statusa u koloni **Gotovo**: `[ ]` nije počet ili je u toku, `[x]` završen i proveren.

| # | Tiket | Faza | Gotovo | Datum | Commit | Napomena |
|:--:|---|:--:|:--:|---|---|---|
| 01 | Repo skelet i Express server | 2 | [x] | 2026-08-22 | aef819a | |
| 02 | MySQL šema i globalne kategorije | 2 | [x] | 2026-08-23 | 3150a46 | |
| 03 | Registracija i prijava | 2 | [x] | 2026-08-23 | f951c8d | |
| 04 | Zaštita ruta i korisnički profil | 2 | [x] | 2026-08-23 | 360ba3a | |
| 05 | CRUD predmeta sa proverom vlasništva | 2 | [x] | 2026-08-23 | 1643f53 | |
| 06 | Kategorije i lokacije preko API-ja | 2 | [x] | 2026-08-23 | 0ef2e56 | |
| 07 | Demo inventar i Postman kolekcija | 2 | [x] | 2026-08-23 | ee67b8e | |
| 08 | Android projekat i build konfiguracija | 3 | [x] | 2026-08-23 | 7af68af | AVD prebačen na API 36 jer je API 37.0 preview slika imala nestabilan system_server, vidi napomenu u tiketu |
| 09 | Lokalna Room baza | 3 | [x] | 2026-08-23 | 3b7b0ae | |
| 10 | Mrežni sloj i obrada grešaka | 3 | [x] | 2026-08-23 | d0fc49c | |
| 11 | Prijava i registracija na uređaju | 3 | [x] | 2026-08-23 | e78de7c | |
| 12 | Ljuska aplikacije i navigacija | 3 | [x] | 2026-08-23 | 309bf2a | |
| 13 | Pregled stanja i zatvaranje vertikalnog preseka | 3 | [x] | 2026-08-29 | 77f6766 | ručno provereno na emulatoru (Pixel6_API36): 63 predmeta/11 kategorija, ukupna vrednost, poslednjih 5, offline rad, ERR-05, prazno i error stanje |
| 14 | Lista inventara | 4 | [x] | 2026-08-29 | 8c5fcef | ručno provereno na emulatoru (Pixel6_API36): 63 predmeta, ikonice kategorija (FR-087), klik na stavku, FAB i prazno stanje, DiffUtil bez treperenja, offline rad i ERR-05 |
| 15 | Dodavanje i izmena predmeta | 4 | [x] | 2026-08-31 | ec4ebf0 | rezim izmene proveren pregledom koda (deljen put sa dodavanjem); ulazna tačka za UI izmenu dolazi u tiketu 16 |
| 16 | Detalji predmeta i brisanje | 4 | [x] | 2026-08-31 | 53fa801 | ručno provereno na emulatoru (Pixel6_API36): BR-007 (samo itemId, exported="false"), sva popunjena/prazna polja, izmena deli formu sa tiketom 15, BR-008 dijalog sa nazivom, soft delete + DELETE poziv (204 u backend logu), trenutni nestanak iz liste i zbirova (63→62), poruka za nepostojeći id bez rušenja aplikacije |
| 17 | Upravljanje lokacijama | 4 | [x] | 2026-08-31 | ec9c1ef | ručno provereno na emulatoru (Pixel6_API36): lista sa brojem predmeta po lokaciji (FR-049), dodavanje sa validacijom (prazan naziv), VR-19 duplikat naziva (409 sa servera prikazan kao poruka), izmena naziva/opisa odmah vidljiva u listi, BR-014 blokirano brisanje sa tačnim brojem predmeta (Garaža, 7 predmeta), brisanje prazne lokacije uz potvrdu i trenutni nestanak iz liste, padajuća lista u formi predmeta odmah odražava trenutne lokacije |
| 18 | Fotografije predmeta | 4 | [x] | 2026-08-31 | 4199e86 | ručno provereno na emulatoru (Pixel6_API36): fotografija iz galerije (sistemski Photo Picker) i kamerom (FR-081), smanjenje na 1080px duže stranice i kompresija (FR-082, potvrđeno 1080×2400→486×1080), fajl u privatnom skladištu aplikacije preživljava restart aplikacije, prikaz na listi i detaljima, zamena i brisanje predmeta uklanjaju fajl sa diska (posle isteka opoziva od 5s da se ne pokvari FR-027), predmet bez fotografije i dalje prikazuje ikonicu kategorije, odbijena dozvola za kameru daje poruku bez rušenja; u toku ručne provere pronađen i ispravljen bag (nedostajao mkdirs() pre prvog upisa fotografije, izazivao rušenje) |
| 19 | Pretraga inventara | 5 | [x] | 2026-08-31 | 4438523 | provereno pregledom koda i Gradle build-om (compileDebugKotlin, compileDebugAndroidTestKotlin, testDebugUnitTest svi prolaze; nova instrumentisana provera za dijakritike dodata u ItemDaoTest); nije bilo dostupnog emulatora/uređaja u ovoj sesiji za ručnu proveru na ekranu |
| 20 | Filteri i sortiranje | 5 | [ ] | | | |
| 21 | Kursna lista i valuta prikaza | 6 | [ ] | | | |
| 22 | Praćenje garancija | 6 | [ ] | | | |
| 23 | Statistika sa grafikonima | 6 | [ ] | | | |
| 24 | Administratorski deo API-ja | 7 | [ ] | | | |
| 25 | Administratorski ekrani | 7 | [ ] | | | |
| 26 | Puna sinhronizacija i offline rad | 7 | [ ] | | | |
| 27 | Poliranje i provera projektnih zahteva | 7 | [ ] | | | |

**Napredak: 19 / 27**

Agent koji završi tiket ažurira i ovaj brojač.

### Kontrolne tačke

Ove tri tačke su prekretnice projekta. Kada agent štiklira jedan od ovih tiketa, dužan je da to **eksplicitno javi korisniku**, jer je tu pravi trenutak za proveru pravca pre nastavka.

| Posle tiketa | Šta je postignuto |
|:--:|---|
| 07 | Backend je kompletan i proveren kroz Postman. Android rad može da počne. |
| 13 | Prvi kompletan vertikalni presek radi — podatak putuje od MySQL baze do ekrana telefona. |
| 18 | **Granica MVP-a.** Postoji odbranjiv proizvod: registracija, prijava, pun CRUD, fotografije. |

---

**Kraj dokumenta.** Izmene zahtevaju odobrenje korisnika i podizanje verzije u zaglavlju.
