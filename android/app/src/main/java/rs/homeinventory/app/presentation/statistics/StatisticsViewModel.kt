package rs.homeinventory.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.local.dao.CategoryAggregate
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.effectiveValueMinor
import rs.homeinventory.app.data.local.prefs.WarrantyPreferences
import rs.homeinventory.app.data.remote.mapper.DateMapper
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.CurrencyRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.domain.model.WarrantyStatus
import rs.homeinventory.app.domain.util.CurrencyConverter
import rs.homeinventory.app.domain.util.MoneyFormatter
import rs.homeinventory.app.domain.util.WarrantyCalculator
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import rs.homeinventory.app.util.WARRANTY_THRESHOLD_DEFAULT_DAYS
import javax.inject.Inject

// SCR-08 — kao Dashboard, ekran cita iskljucivo iz Room-a (FR-078); jedini asinhroni poziv je kursna
// lista, i ona pada na lokalni kes bez interneta (CurrencyRepository). BR-017 Error stanje je odbrambeno
// (Room citanje/obrada normalno ne baca) — pokriva NFR-11 ako se ipak nesto neocekivano desi (tiket 27).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val currencyRepository: CurrencyRepository,
    private val warrantyPreferences: WarrantyPreferences,
    authRepository: AuthRepository,
    private val errorMessageProvider: ErrorMessageProvider
) : ViewModel() {

    private val currentUser = authRepository.currentUser.filterNotNull()
    private val retryTrigger = MutableStateFlow(0)

    private val aggregates: StateFlow<List<CategoryAggregate>> = currentUser
        .flatMapLatest { itemRepository.observeCategoryAggregates(it.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allItems: StateFlow<List<ItemListRow>> = currentUser
        .flatMapLatest { itemRepository.observeAllItems(it.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val warrantyThreshold: StateFlow<Int> = warrantyPreferences.thresholdDays
        .stateIn(viewModelScope, SharingStarted.Eagerly, WARRANTY_THRESHOLD_DEFAULT_DAYS)

    // BR-013 — prazna mapa znaci da nijedan kurs jos nije dostupan; ne racuna se kao kurs 1.0.
    private val rates = MutableStateFlow<Map<String, Double>>(emptyMap())

    val state: StateFlow<UiState<StatisticsUi>> = combine(
        combine(currentUser, aggregates, rates, ::Triple),
        allItems,
        warrantyThreshold,
        retryTrigger
    ) { base, items, threshold, _ ->
        val (user, categoryAggregates, rateMap) = base
        if (categoryAggregates.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(buildStatisticsUi(user, categoryAggregates, items, rateMap, threshold))
        }
    }
        .catch { emit(UiState.Error(errorMessageProvider.message(ErrorCode.UNKNOWN))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() {
        retryTrigger.value++
    }

    init {
        // Zaseban coroutine — kursna lista se povlaci nezavisno, ekran radi i dok ona jos ne stigne
        // (rates ostaje prazna mapa dok se ne ucita, isti obrazac kao DashboardViewModel, tiket 21/22).
        viewModelScope.launch {
            val result = currencyRepository.getRates()
            if (result is Resource.Success) rates.value = result.data
        }
    }

    // BR-011 — efektivna vrednost. BR-009 — svaki zbir prvo konvertuje u valutu prikaza.
    private fun buildStatisticsUi(
        user: User,
        aggregates: List<CategoryAggregate>,
        items: List<ItemListRow>,
        rates: Map<String, Double>,
        warrantyThresholdDays: Int
    ): StatisticsUi {
        val totalItemCount = aggregates.sumOf { it.itemCount }

        val totalByCurrency = aggregates.groupBy { it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.totalMinor } }
        val (totalValueMinor, unconverted) = sumConverted(totalByCurrency, user.currency, rates)

        val averageValueMinor = if (totalItemCount > 0) totalValueMinor / totalItemCount else 0L

        val categoryStats = aggregates.groupBy { it.categoryId to it.categoryName }
            .map { (key, rows) ->
                val byCurrency = rows.groupBy { it.currency }.mapValues { (_, r) -> r.sumOf { it.totalMinor } }
                val (valueMinor, _) = sumConverted(byCurrency, user.currency, rates)
                CategoryStatUi(
                    categoryName = key.second,
                    itemCount = rows.sumOf { it.itemCount },
                    valueMinor = valueMinor,
                    valueFormatted = MoneyFormatter.format(valueMinor, user.currency)
                )
            }
            .sortedByDescending { it.valueMinor }

        return StatisticsUi(
            totalItemCount = totalItemCount,
            totalValueFormatted = MoneyFormatter.format(totalValueMinor, user.currency),
            categoryCount = categoryStats.size,
            averageValueFormatted = MoneyFormatter.format(averageValueMinor, user.currency),
            unconvertedAmounts = unconverted.map { (currency, amountMinor) ->
                UnconvertedAmountUi(currency, MoneyFormatter.format(amountMinor, currency))
            },
            categoryStats = categoryStats,
            mostExpensiveItem = findMostExpensiveItem(items, user.currency, rates),
            warrantyBreakdown = buildWarrantyBreakdown(items, warrantyThresholdDays)
        )
    }

    // BR-009 — poredjenje ide iskljucivo preko valute prikaza; predmet cija valuta nema kurs se ne
    // moze pravedno uporediti pa se izostavlja iz izbora najskupljeg (isti duh kao sortiranje u tiketu 20).
    private fun findMostExpensiveItem(
        items: List<ItemListRow>,
        displayCurrency: String,
        rates: Map<String, Double>
    ): MostExpensiveItemUi? = items
        .mapNotNull { row ->
            val converted = CurrencyConverter.convert(row.effectiveValueMinor(), row.currency, displayCurrency, rates)
                ?: return@mapNotNull null
            row to converted
        }
        .maxByOrNull { it.second }
        ?.let { (row, convertedMinor) ->
            MostExpensiveItemUi(
                name = row.name,
                categoryName = row.categoryName,
                valueFormatted = MoneyFormatter.format(convertedMinor, displayCurrency)
            )
        }

    // BR-010 — status se racuna isto kao na Dashboard-u/listi inventara (tiket 22); uvek sve cetiri
    // grane prisutne, i sa brojem 0, da raspodela ne menja oblik izmedju osvezavanja.
    private fun buildWarrantyBreakdown(items: List<ItemListRow>, thresholdDays: Int): List<WarrantyBreakdownUi> {
        val today = LocalDate.now()
        val counts = items
            .map { WarrantyCalculator.status(DateMapper.parseLocalDate(it.warrantyExpirationDate), thresholdDays, today) }
            .groupingBy { it }
            .eachCount()
        return WarrantyStatus.entries.map { status -> WarrantyBreakdownUi(status, counts[status] ?: 0) }
    }

    // BR-013 — nedostupan kurs se prijavljuje, ne pretpostavlja (kurs 1.0 se nikad ne koristi).
    private fun sumConverted(
        amountsByCurrency: Map<String, Long>,
        displayCurrency: String,
        rates: Map<String, Double>
    ): Pair<Long, Map<String, Long>> {
        var convertedTotal = 0L
        val unconverted = mutableMapOf<String, Long>()
        amountsByCurrency.forEach { (currency, amountMinor) ->
            val converted = CurrencyConverter.convert(amountMinor, currency, displayCurrency, rates)
            if (converted != null) convertedTotal += converted else unconverted[currency] = amountMinor
        }
        return convertedTotal to unconverted
    }
}
