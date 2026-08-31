package rs.homeinventory.app.util

// C-04 — bez magicnih brojeva u kodu.
const val DASHBOARD_RECENT_ITEMS_LIMIT = 5

// tech.md sekcija 10 — Intent extra za ItemDetailsActivity; ceo predmet se ne prosledjuje (BR-007).
const val EXTRA_ITEM_ID = "extra_item_id"

// SCR-06 — VR-12, valute podrzane na klijentu.
val SUPPORTED_CURRENCIES = listOf("RSD", "EUR", "USD", "CHF", "GBP", "BAM")

// Fragment Result API — AddEditItemFragment vraca rezultat pozivajucem ekranu (tech.md sekcija 8.6).
const val RESULT_ITEM_SAVED = "result_item_saved"
const val RESULT_ITEM_SAVED_ID = "result_item_saved_id"

// Activity Result — ItemDetailsActivity vraca id obrisanog predmeta pozivajucem ekranu (tiket 16).
const val EXTRA_ITEM_DELETED_ID = "extra_item_deleted_id"

// FR-027 — trajanje opoziva brisanja predmeta.
const val DELETE_UNDO_DURATION_MS = 5_000

// FR-032 — zadrska pretrage pre osvezavanja rezultata.
const val SEARCH_DEBOUNCE_MS = 300L

// FR-037/BR-010 — podrazumevani prag "garancija uskoro istice" za filter u listi inventara, dok
// podesivi prag po korisniku (FR-051, tiket 22) ne postoji.
const val WARRANTY_EXPIRING_SOON_DEFAULT_DAYS = 30L
