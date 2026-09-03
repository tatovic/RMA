-- 002 — users.token_version: poništavanje ranije izdatih tokena pri promeni lozinke.
--
-- Zašto (tiket 28, nalaz 12): do sada promena lozinke nije uticala ni na jedan već izdat JWT —
-- token ukraden pre promene ostajao je važeći do isteka (JWT_EXPIRES_IN, podrazumevano 7 dana).
-- Vrednost se nosi kao `tv` claim u tokenu i poredi se u middleware/authenticate.js uz postojeću
-- proveru is_active; changePassword je uvećava, čime svi stariji tokeni istog korisnika propadaju.

ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER currency;
