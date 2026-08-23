const { z } = require('zod');

const SUPPORTED_CURRENCIES = require('../../utils/currencies');

// BR-012 — oba polja opciona, prisutna se validiraju.
const updateProfileSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'Ime mora imati najmanje 2 znaka')
    .max(100, 'Ime može imati najviše 100 znakova')
    .optional(),
  currency: z.enum(SUPPORTED_CURRENCIES, { errorMap: () => ({ message: 'Nepodržana valuta' }) }).optional(),
});

// VR-04 — nova lozinka prolazi istu validaciju kao pri registraciji.
const changePasswordSchema = z.object({
  currentPassword: z
    .string({ required_error: 'Trenutna lozinka je obavezna' })
    .min(1, 'Trenutna lozinka je obavezna'),
  newPassword: z
    .string({ required_error: 'Nova lozinka je obavezna' })
    .min(8, 'Lozinka mora imati najmanje 8 znakova'),
});

module.exports = { updateProfileSchema, changePasswordSchema };
