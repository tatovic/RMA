# 07: Demo inventar i Postman kolekcija

**What to build:** Sistem ima realističan demo nalog sa oko šezdeset predmeta, pa pretraga, filteri i grafikoni od prvog dana imaju šta da prikažu. Uz to postoji kolekcija zahteva kojom se ceo API proverava bez pisanja koda — to je i dokaz da backend radi pre nego što se dotakne Android.

**Blocked by:** 06 — Kategorije i lokacije preko API-ja.

**Status:** ready-for-agent

- [ ] Seed kreira demo nalog sa poznatim kredencijalima, zapisanim u README fajlu
- [ ] Ubacuje se oko šezdeset predmeta raspoređenih po svih jedanaest kategorija i svih devet lokacija
- [ ] Valute su izmešane po raspodeli iz db.md sekcija 4.3, da bi konverzija imala smisla
- [ ] Statusi garancija su izmešani: aktivne, one koje ističu u narednih trideset dana, istekle i one bez datuma
- [ ] Datumi kupovine su raspoređeni kroz poslednje četiri godine, da filter po godini ima smisla
- [ ] Serijski brojevi su popunjeni kod elektronike i bele tehnike, prazni drugde
- [ ] Seed je idempotentan: dvostruko pokretanje daje isti rezultat, bez duplikata
- [ ] Seed ne dira stvarne korisničke naloge, samo demo nalog
- [ ] Postman kolekcija pokriva svaki endpoint, sa promenljivom za token koja se puni automatski posle prijave
- [ ] Kolekcija sadrži scenario vlasništva: korisnik A kreira predmet, korisnik B ga traži i dobija 404
- [ ] Kolekcija je commitovana u repozitorijum
