# 13: Pregled stanja i zatvaranje vertikalnog preseka

**What to build:** Korisnik se prijavi i odmah vidi koliko stvari poseduje i koliko one ukupno vrede. Podatak koji je unet kroz Postman u demo seed sada putuje kroz ceo sistem sve do ekrana telefona. Ovim se zatvara prvi kompletan vertikalni presek kroz sve slojeve.

**Blocked by:** 12 — Ljuska aplikacije i navigacija.

**Status:** done

- [x] Pri otvaranju se kategorije, lokacije i predmeti povlače sa servera i upisuju u lokalnu bazu
- [x] Ekran prikazuje ukupan broj predmeta kao zbir količina, ne kao broj redova
- [x] Ekran prikazuje ukupnu vrednost inventara u valuti korisnika, po formuli iz BR-011
- [x] Prikazuje se broj predmeta po kategorijama
- [x] Prikazuje se poslednjih pet dodatih predmeta
- [x] Ekran čita isključivo iz lokalne baze; mreža samo puni bazu, a prikaz se osvežava sam kad podaci stignu
- [x] Ekran radi bez interneta ako su podaci ranije preuzeti
- [x] Prazan inventar prikazuje poziv na akciju, ne praznu belinu (BR-017)
- [x] Postoje sva četiri stanja ekrana: učitavanje, sadržaj, prazno i greška
- [x] Greška mreže uz postojeće lokalne podatke prikazuje kratku poruku, a ne ekran greške (ERR-05)
- [x] Prijava demo naloga prikazuje oko šezdeset predmeta iz seed skripte

Napomena: konverzija valuta (BR-009 pun kurs) nije deo ovog tiketa — dok tiket 21 ne uvede kursnu listu, predmeti u drugoj valuti od korisnikove se izdvajaju iz ukupne vrednosti i prikazuje se napomena, po duhu BR-013.

Provereno ručno na emulatoru (Pixel6_API36) 2026-08-29: prijava demo nalogom prikazuje 63 predmeta (zbir količina) u 11 kategorija i ukupnu vrednost 3.867.253,00 RSD sa napomenom o nekonvertovanim valutama; poslednjih pet dodatih predmeta prikazano ispravno; pull-to-refresh radi; gašenje mreže (adb) pokazuje da podaci ostaju vidljivi i da refresh greška ide kao kratka poruka (ERR-05); nov nalog bez predmeta prikazuje prazno stanje sa CTA dugmetom; stanje greške potvrđeno (nevažeći token posle re-seed-a prikazao je pun error ekran sa porukom i dugmetom za ponovni pokušaj).
