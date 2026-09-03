-- 001 — inventory_items: strani ključevi ka categories/locations prelaze sa RESTRICT na CASCADE.
--
-- Zašto (tiket 28, blokirajući nalaz 02): soft-obrisan predmet (deleted_at IS NOT NULL) ostaje red u
-- tabeli i drži strani ključ u životu. BR-014 guard broji samo ŽIVE predmete, pa bi propustio
-- brisanje, a MySQL bi ga zatim odbio sa ER_ROW_IS_REFERENCED_2 — 500 na sasvim običnom toku:
-- "dodaj predmet u sobu, obriši predmet, obriši sobu".
--
-- Kaskada je bezbedna upravo zato što guard ostaje: dok postoji ijedan živ predmet, brisanje se
-- odbija sa LOCATION_IN_USE/CATEGORY_IN_USE i kaskada se nikada ne izvršava. Kada guard propusti,
-- kaskada može da dohvati samo tombstone redove.
--
-- schema.sql je pisan sa CREATE TABLE IF NOT EXISTS, pa postojeće baze ne pokupe izmenu iz njega —
-- zato ovaj fajl. Pokreće se sa `npm run db:migrate`.

ALTER TABLE inventory_items DROP FOREIGN KEY fk_items_category;
ALTER TABLE inventory_items
  ADD CONSTRAINT fk_items_category
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE;

ALTER TABLE inventory_items DROP FOREIGN KEY fk_items_location;
ALTER TABLE inventory_items
  ADD CONSTRAINT fk_items_location
  FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE;
