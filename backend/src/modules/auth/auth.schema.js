const { z } = require('zod');

// VR-01 do VR-05 — prd.md sekcija 12.
const registerSchema = z
  .object({
    name: z
      .string({ required_error: 'Ime je obavezno' })
      .trim()
      .min(2, 'Ime mora imati najmanje 2 znaka')
      .max(100, 'Ime može imati najviše 100 znakova'),
    email: z
      .string({ required_error: 'Email je obavezan' })
      .trim()
      .max(255, 'Email može imati najviše 255 znakova')
      .email('Unesite ispravnu email adresu')
      .toLowerCase(),
    password: z
      .string({ required_error: 'Lozinka je obavezna' })
      .min(8, 'Lozinka mora imati najmanje 8 znakova'),
    confirmPassword: z.string({ required_error: 'Potvrda lozinke je obavezna' }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Lozinke se ne poklapaju',
    path: ['confirmPassword'],
  });

const loginSchema = z.object({
  email: z
    .string({ required_error: 'Email je obavezan' })
    .trim()
    .min(1, 'Email je obavezan')
    .toLowerCase(),
  password: z.string({ required_error: 'Lozinka je obavezna' }).min(1, 'Lozinka je obavezna'),
});

module.exports = { registerSchema, loginSchema };
