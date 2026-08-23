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

// Polja iz db.md sekcija 3 (tabela categories) kojima admin upravlja.
const categoryFields = {
  name: z
    .string({ required_error: 'Naziv je obavezan' })
    .trim()
    .min(1, 'Naziv je obavezan')
    .max(60, 'Naziv može imati najviše 60 znakova'),
  description: optionalText(255, 'Opis'),
  iconKey: optionalText(40, 'Ključ ikonice'),
  sortOrder: z
    .number({ invalid_type_error: 'Redosled mora biti broj' })
    .int('Redosled mora biti ceo broj')
    .min(0, 'Redosled ne može biti negativan')
    .default(0),
};

// id generiše server — db.md sekcija 2.1.
const createCategorySchema = z.object({ ...categoryFields });

const updateCategorySchema = z.object({ ...categoryFields });

module.exports = { createCategorySchema, updateCategorySchema };
