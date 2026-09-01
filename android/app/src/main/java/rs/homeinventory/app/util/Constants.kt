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

// FR-051/FR-052 — podrazumevani i ponudjeni pragovi "garancija uskoro istice" (BR-010), koriste ih
// i filter u listi inventara (FR-037) i podesavanje u Profilu (tiket 22).
const val WARRANTY_THRESHOLD_DEFAULT_DAYS = 30
val WARRANTY_THRESHOLD_OPTIONS = listOf(7, 30, 60, 90)

// FR-098 — kljuc u sync_metadata pod kojim SyncManager cuva serversko vreme poslednjeg delta pull-a (tiket 26).
const val SYNC_METADATA_ITEMS_LAST_SYNC_KEY = "items_last_sync_at"
