package rs.homeinventory.app.presentation.statistics

import rs.homeinventory.app.domain.model.WarrantyStatus

// SCR-08 — model ekrana; agregacija ide u StatisticsViewModel iz CategoryAggregate/ItemListRow (BR-009, BR-011).
data class StatisticsUi(
    val totalItemCount: Int,
    val totalValueFormatted: String,
    val categoryCount: Int,
    val averageValueFormatted: String,
    // BR-013 — iznosi cije valute nemaju kurs, prijavljeni odvojeno umesto tihog pogresnog sabiranja.
    val unconvertedAmounts: List<UnconvertedAmountUi>,
    // Sortirano po vrednosti opadajuce — deli ga pie chart, bar chart i tabela.
    val categoryStats: List<CategoryStatUi>,
    val mostExpensiveItem: MostExpensiveItemUi?,
    // Uvek svo cetiri statusa (BR-010), i kad je broj 0, da se raspodela ne menja oblik.
    val warrantyBreakdown: List<WarrantyBreakdownUi>
)

data class CategoryStatUi(
    val categoryName: String,
    val itemCount: Int,
    val valueMinor: Long,
    val valueFormatted: String
)

data class UnconvertedAmountUi(
    val currency: String,
    val amountFormatted: String
)

data class MostExpensiveItemUi(
    val name: String,
    val categoryName: String,
    val valueFormatted: String
)

data class WarrantyBreakdownUi(
    val status: WarrantyStatus,
    val itemCount: Int
)
