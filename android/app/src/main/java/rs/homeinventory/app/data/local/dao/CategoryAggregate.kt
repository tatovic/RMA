package rs.homeinventory.app.data.local.dao

// Zbir se namerno grupise i po valuti (BR-009) - SQL ne sme sabrati razlicite valute.
// Konverziju u valutu prikaza radi Kotlin sloj.
data class CategoryAggregate(
    val categoryId: String,
    val categoryName: String,
    val currency: String,
    val itemCount: Int,        // zbir quantity
    val totalMinor: Long       // zbir efektivne vrednosti u toj valuti
)
