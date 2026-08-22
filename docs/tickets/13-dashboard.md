# 13: Pregled stanja i zatvaranje vertikalnog preseka

**What to build:** Korisnik se prijavi i odmah vidi koliko stvari poseduje i koliko one ukupno vrede. Podatak koji je unet kroz Postman u demo seed sada putuje kroz ceo sistem sve do ekrana telefona. Ovim se zatvara prvi kompletan vertikalni presek kroz sve slojeve.

**Blocked by:** 12 — Ljuska aplikacije i navigacija.

**Status:** ready-for-agent

- [ ] Pri otvaranju se kategorije, lokacije i predmeti povlače sa servera i upisuju u lokalnu bazu
- [ ] Ekran prikazuje ukupan broj predmeta kao zbir količina, ne kao broj redova
- [ ] Ekran prikazuje ukupnu vrednost inventara u valuti korisnika, po formuli iz BR-011
- [ ] Prikazuje se broj predmeta po kategorijama
- [ ] Prikazuje se poslednjih pet dodatih predmeta
- [ ] Ekran čita isključivo iz lokalne baze; mreža samo puni bazu, a prikaz se osvežava sam kad podaci stignu
- [ ] Ekran radi bez interneta ako su podaci ranije preuzeti
- [ ] Prazan inventar prikazuje poziv na akciju, ne praznu belinu (BR-017)
- [ ] Postoje sva četiri stanja ekrana: učitavanje, sadržaj, prazno i greška
- [ ] Greška mreže uz postojeće lokalne podatke prikazuje kratku poruku, a ne ekran greške (ERR-05)
- [ ] Prijava demo naloga prikazuje oko šezdeset predmeta iz seed skripte
