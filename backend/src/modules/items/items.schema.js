const { z } = require('zod');

const SUPPORTED_CURRENCIES = require('../../utils/currencies');

// VR-10 — maksimum za novčane vrednosti u minor jedinicama.
const MAX_MONEY_MINOR = 99999999999;

const uuidField = (label) =>
  z.string({ required_error: `${label} je obavezan` }).uuid(`${label} mora biti validan UUID`);

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

// VR-20 — kalendarski datum, uvek YYYY-MM-DD (db.md sekcija 2.3A).
const dateOnlyField = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'Datum mora biti u formatu YYYY-MM-DD')
  .nullable()
  .optional()
  .transform(emptyToNull);

const moneyField = z
  .number({ invalid_type_error: 'Iznos mora biti broj u minor jedinicama' })
  .int('Iznos mora biti ceo broj (nema decimala) — db.md sekcija 2.2')
  .min(0, 'Iznos ne može biti negativan')
  .max(MAX_MONEY_MINOR, 'Iznos je prevelik')
  .nullable()
  .optional();

// Polja iz db.md sekcija 7 kojima klijent upravlja pri kreiranju i izmeni.
// userId namerno NIJE ovde — OWN-02, server ga uvek uzima iz tokena, telo se ignoriše.
const itemFields = {
  name: z
    .string({ required_error: 'Naziv je obavezan' })
    .trim()
    .min(1, 'Naziv je obavezan')
    .max(120, 'Naziv može imati najviše 120 znakova'),
  description: optionalText(1000, 'Opis'),
  categoryId: uuidField('Kategorija'),
  locationId: uuidField('Lokacija'),
  manufacturer: optionalText(100, 'Proizvođač'),
  model: optionalText(100, 'Model'),
  serialNumber: optionalText(100, 'Serijski broj'),
  quantity: z
    .number({ invalid_type_error: 'Količina mora biti broj' })
    .int('Količina mora biti ceo broj')
    .min(1, 'Količina mora biti najmanje 1')
    .max(9999, 'Količina može biti najviše 9999')
    .default(1),
  purchasePrice: moneyField,
  estimatedValue: moneyField,
  currency: z
    .enum(SUPPORTED_CURRENCIES, { errorMap: () => ({ message: 'Nepodržana valuta' }) })
    .default('RSD'),
  purchaseDate: dateOnlyField,
  warrantyExpirationDate: dateOnlyField,
  seller: optionalText(100, 'Prodavac'),
  notes: optionalText(1000, 'Napomena'),
};

// Kreiranje prihvata identifikator koji generiše klijent (db.md sekcija 2.1).
const createItemSchema = z.object({
  id: z.string({ required_error: 'Identifikator je obavezan' }).uuid('Identifikator mora biti validan UUID'),
  ...itemFields,
});

// Izmena nosi updatedAt sa klijenta radi poređenja pri sinhronizaciji (DB-RULE-04).
const updateItemSchema = z.object({
  ...itemFields,
  updatedAt: z
    .string({ required_error: 'updatedAt je obavezan' })
    .datetime({ message: 'updatedAt mora biti ISO-8601 datum i vreme (UTC, sa Z)' }),
});

// Delta pull (FR-098) — ?since=<ISO-8601>.
const listItemsQuerySchema = z.object({
  since: z
    .string()
    .datetime({ message: 'since mora biti ISO-8601 datum i vreme (UTC, sa Z)' })
    .optional(),
});

module.exports = { createItemSchema, updateItemSchema, listItemsQuerySchema };
