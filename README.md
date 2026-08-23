# Home Inventory

Android aplikacija za evidenciju, organizaciju i upravljanje kućnim inventarom, sa sopstvenim REST API-jem i lokalnom bazom na uređaju.

Korisnik vodi evidenciju stvari koje poseduje: šta ima, u kojoj prostoriji se nalazi, koliko je platio, koliko danas vredi i do kada mu traje garancija. Aplikacija sabira ukupnu vrednost imovine, konvertuje iznose iz različitih valuta preko kursne liste i upozorava kada neka garancija ističe.

> Vodeći princip razvoja: **Build a real product, not a demo CRUD application.**

## Dokumentacija

Projekat se vodi kroz tri dokumenta. Pročitaj ih pre bilo kakvog rada na kodu.

| Dokument | Odgovara na pitanje |
|---|---|
| [prd.md](prd.md) | ŠTA proizvod radi, za koga i po kojim pravilima |
| [db.md](db.md) | GDE i KAKO podaci žive — MySQL šema, Room šema, mapiranja |
| [tech.md](tech.md) | KAKO se gradi — stack, slojevi, API kontrakt, algoritmi, konvencije |

**Napredak implementacije se prati u [prd.md, sekcija 16](prd.md#16-napredak-implementacije).**

## Tiketi

Rad je podeljen na 27 tiketa u [docs/tickets/](docs/tickets/), povezanih u **strogo linearan lanac**: tiket N je blokiran tiketom N-1. Radi se odozgo nadole, jedan po jedan.

[Indeks tiketa](docs/tickets/README.md)

Tri prekretnice: posle tiketa **07** backend je gotov, posle **13** radi prvi kompletan vertikalni presek, posle **18** postoji odbranjiv MVP.

## Tehnologije

**Android** — Kotlin, XML layouti sa Fragmentima, ViewBinding, Material 3, Navigation Component, ViewModel, StateFlow, Room, DataStore, Retrofit, OkHttp, Hilt, Coroutines, Glide, MPAndroidChart

**Backend** — Node.js, Express, MySQL 8, JWT, BCrypt, zod

**Eksterni API** — [open.er-api.com](https://open.er-api.com) za kursnu listu, bez API ključa

**Arhitektura** — MVVM + Repository pattern, offline-first sa Room bazom kao lokalnim izvorom istine

## Struktura

```
RMA/
├── prd.md, db.md, tech.md    dokumentacija
├── docs/tickets/             27 tiketa
├── backend/                  Node.js REST API
└── android/                  Android aplikacija
```

## Pokretanje

Detaljno uputstvo je u [tech.md, sekcija 16](tech.md#16-pokretanje-projekta). Ukratko:

```bash
cd backend
npm install
cp .env.example .env     # popuniti DB_PASSWORD i JWT_SECRET
npm run db:create
npm run seed
npm run dev
```

Android projekat se otvara u Android Studiju iz foldera `android/`. Emulator dolazi do backenda preko `10.0.2.2:3000`.

## Demo nalog

`npm run seed` puni bazu demo inventarom od šezdesetak predmeta, raspoređenih po svim kategorijama i lokacijama.

| Polje | Vrednost |
|---|---|
| Email | `demo@homeinventory.rs` |
| Lozinka | `Demo1234` |

Seed je idempotentan — ponovno pokretanje briše i ponovo kreira samo ovaj nalog, bez uticaja na ostale korisnike.

## Postman kolekcija

[backend/postman/home-inventory.postman_collection.json](backend/postman/home-inventory.postman_collection.json) pokriva svaki endpoint API-ja, uključujući scenario provere vlasništva (BR-002). Uvesti u Postman i pokrenuti kao Collection Run odozgo nadole — promenljiva `token` se puni automatski posle registracije/prijave.

## Napomena o bezbednosti

Repozitorijum je **javan**. `.env` se nikada ne commituje — u repo ide samo `.env.example` sa praznim vrednostima. Puna pravila su u [tech.md, sekcija 14](tech.md#14-git-strategija).

Tokom razvoja se koristi nešifrovan HTTP ka lokalnoj adresi. Produkcijsko okruženje zahteva HTTPS.

## Autor

Marko Tatović — projekat iz predmeta Razvoj mobilnih aplikacija
