package rs.homeinventory.app.presentation.dashboard

// SCR-03 — model ekrana; agregacija ide u DashboardViewModel iz CategoryAggregate/ItemListRow (BR-009, BR-011).
data class DashboardUi(
    val totalItemCount: Int,
    val totalValueFormatted: String,
    val hasUnconvertedCurrencies: Boolean,
    val categoryCounts: List<CategoryCountUi>,
    val recentItems: List<RecentItemUi>
)

data class CategoryCountUi(
    val categoryName: String,
    val itemCount: Int
)

data class RecentItemUi(
    val id: String,
    val name: String,
    val categoryName: String,
    val locationName: String,
    val valueFormatted: String
)
