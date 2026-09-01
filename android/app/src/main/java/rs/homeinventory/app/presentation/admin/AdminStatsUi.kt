package rs.homeinventory.app.presentation.admin

// SCR-11 — pregled sistema (tiket 25): brojevi po statusu korisnika, ukupan broj predmeta i kategorija.
data class AdminStatsUi(
    val registeredUsers: Int,
    val activeUsers: Int,
    val deactivatedUsers: Int,
    val totalItems: Int,
    val totalCategories: Int
)
