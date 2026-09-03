// Katalog gresaka: prd.md sekcija 10
const ErrorCodes = {
  VALIDATION_ERROR: { status: 400, message: 'Neispravni podaci' },
  INVALID_CREDENTIALS: { status: 401, message: 'Pogrešan email ili lozinka' },
  TOKEN_EXPIRED: { status: 401, message: 'Sesija je istekla. Prijavite se ponovo.' },
  TOKEN_INVALID: { status: 401, message: 'Sesija nije važeća. Prijavite se ponovo.' },
  ACCOUNT_DEACTIVATED: { status: 403, message: 'Vaš nalog je deaktiviran. Obratite se administratoru.' },
  FORBIDDEN: { status: 403, message: 'Nemate dozvolu za ovu akciju.' },
  NOT_FOUND: { status: 404, message: 'Traženi podatak ne postoji.' },
  EMAIL_ALREADY_EXISTS: { status: 409, message: 'Nalog sa ovom email adresom već postoji.' },
  CATEGORY_NAME_TAKEN: { status: 409, message: 'Kategorija sa ovim nazivom već postoji.' },
  LOCATION_NAME_TAKEN: { status: 409, message: 'Lokacija sa ovim nazivom već postoji.' },
  CATEGORY_IN_USE: { status: 409, message: 'Kategorija se koristi kod predmeta i ne može se obrisati.' },
  LOCATION_IN_USE: { status: 409, message: 'Lokacija se koristi kod predmeta i ne može se obrisati.' },
  CANNOT_DEACTIVATE_SELF: { status: 409, message: 'Ne možete deaktivirati sopstveni nalog.' },
  ITEM_ID_TAKEN: { status: 409, message: 'Ovaj identifikator predmeta je već zauzet.' },
  LOCATION_ID_TAKEN: { status: 409, message: 'Ovaj identifikator lokacije je već zauzet.' },
  SYNC_CONFLICT: { status: 409, message: 'Podatak je izmenjen na drugom uređaju.' },
  WRONG_CURRENT_PASSWORD: { status: 400, message: 'Trenutna lozinka nije tačna.' },
  INTERNAL_ERROR: { status: 500, message: 'Došlo je do greške. Pokušajte ponovo.' },
};

module.exports = ErrorCodes;
