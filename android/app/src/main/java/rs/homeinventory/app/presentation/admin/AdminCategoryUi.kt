package rs.homeinventory.app.presentation.admin

// SCR-13 — model za prikaz jedne globalne kategorije, sa brojem predmeta svih korisnika (tiket 25).
data class AdminCategoryUi(
    val id: String,
    val name: String,
    val itemCount: Int
)
