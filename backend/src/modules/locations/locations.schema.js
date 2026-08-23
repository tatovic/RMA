const { z } = require('zod');

// BR-016 — prazan string se čuva kao NULL, nikad kao ''.
const emptyToNull = (v) => (v === '' ? null : v);

const optionalText = (max, label) =>
  z
    .string()
    .trim()
    .max(max, `${label} može imati najviše ${max} znakova`)
    .nullable()
    .optional()
    .transform(emptyToNull);

// Polja iz db.md sekcija 3 (tabela locations) kojima korisnik upravlja.
const locationFields = {
  name: z
    .string({ required_error: 'Naziv je obavezan' })
    .trim()
    .min(1, 'Naziv je obavezan')
    .max(60, 'Naziv može imati najviše 60 znakova'),
  description: optionalText(255, 'Opis'),
};

// Kreiranje prihvata identifikator koji generiše klijent (db.md sekcija 2.1).
const createLocationSchema = z.object({
  id: z.string({ required_error: 'Identifikator je obavezan' }).uuid('Identifikator mora biti validan UUID'),
  ...locationFields,
});

const updateLocationSchema = z.object({ ...locationFields });

module.exports = { createLocationSchema, updateLocationSchema };
