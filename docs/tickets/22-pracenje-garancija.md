# 22: Praćenje garancija

**What to build:** Aplikacija prestaje da bude spisak i počinje da bude korisna. Korisnik otvori pregled i pročita da mu garancija za televizor ističe za petnaest dana, pa stigne da reklamira kvar. Granicu do koje se smatra da nešto uskoro ističe bira sam.

**Blocked by:** 21 — Kursna lista i valuta prikaza.

**Status:** ready-for-agent

- [ ] Status garancije se računa iz datuma isteka po pravilu BR-010, u sva četiri slučaja
- [ ] Poređenje je isključivo po kalendarskom datumu, bez vremena i vremenske zone
- [ ] Datum isteka na današnji dan znači da garancija uskoro ističe, ne da je istekla
- [ ] Status se nigde ne čuva u bazi, nego se uvek izvodi (db.md sekcija 2.4)
- [ ] Korisnik bira prag u profilu: 7, 30, 60 ili 90 dana, podrazumevano 30 (FR-051)
- [ ] Izbor praga se pamti između pokretanja i odmah menja prikaz
- [ ] Pregled prikazuje karticu upozorenja, sortiranu po hitnosti (FR-053)
- [ ] Tekst upozorenja je u obliku Garancija za Samsung TV istice za 15 dana (FR-054)
- [ ] Predmet bez datuma garancije se ne pojavljuje među upozorenjima
- [ ] Stavka u listi inventara nosi oznaku statusa garancije
- [ ] Ekran detalja prikazuje status i preostali broj dana
- [ ] Statusi su razdvojeni bojom po tabeli iz BR-010
- [ ] Unit testovi pokrivaju sve četiri grane i granične datume
