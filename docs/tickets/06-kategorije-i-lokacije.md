# 06: Kategorije i lokacije preko API-ja

**What to build:** Korisnik dobija listu kategorija koje sistem nudi i pun nadzor nad prostorijama u svom domu. Kategorije menja samo administrator, jer su zajedničke za sve. Ni kategorija ni lokacija ne mogu nestati ako se za njih još drži neki predmet.

**Blocked by:** 05 — CRUD predmeta sa proverom vlasništva.

**Status:** ready-for-agent

- [ ] Svaki prijavljen korisnik može pročitati listu kategorija, uz podatak koliko ih predmeta koristi
- [ ] Kreiranje, izmenu i brisanje kategorije može samo administrator; korisnik dobija 403
- [ ] Naziv kategorije je jedinstven; duplikat vraća 409 CATEGORY_NAME_TAKEN
- [ ] Korisnik vidi isključivo svoje lokacije i može ih kreirati, menjati i brisati
- [ ] Naziv lokacije je jedinstven u okviru jednog korisnika; dva korisnika smeju imati istu Garažu
- [ ] Rad sa tuđom lokacijom vraća 404
- [ ] Brisanje kategorije ili lokacije koju koristi bar jedan neobrisan predmet vraća 409 sa brojem predmeta koji je blokiraju (BR-014)
- [ ] Poruka o grešci sadrži tačan broj predmeta, da bi korisnik znao koliko posla ga čeka
