package rs.homeinventory.app.presentation.admin

import rs.homeinventory.app.data.local.UserRole

// SCR-12 — spisak korisnika: ime, email, rola, status i broj predmeta (tiket 25, OWN-06 — nikad sadrzaj).
data class AdminUserUi(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val itemCount: Int,
    // BR-004 — admin ne moze da deaktivira sopstveni nalog; ekran to proverava pre potvrde (tiket 25).
    val isSelf: Boolean
)
