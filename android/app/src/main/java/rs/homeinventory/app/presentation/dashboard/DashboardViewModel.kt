package rs.homeinventory.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.local.dao.CategoryAggregate
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.effectiveValueMinor
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.domain.util.MoneyFormatter
import rs.homeinventory.app.util.DASHBOARD_RECENT_ITEMS_LIMIT
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-03 — ekran cita iskljucivo iz Room-a, mreza samo puni bazu (tech.md sekcija 5.3, DEP-02).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val currentUser = authRepository.currentUser.filterNotNull()

    // SharingStarted.Eagerly — mora odrazavati pravo stanje lokalne baze nezavisno od toga
    // da li Fragment vec sluša, jer refresh() proverava aggregates.value pre nego sto UI pretplati state.
    private val aggregates: StateFlow<List<CategoryAggregate>> = currentUser
        .flatMapLatest { itemRepository.observeCategoryAggregates(it.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val recentItems: StateFlow<List<ItemListRow>> = currentUser
        .flatMapLatest { itemRepository.observeRecentItems(it.id, DASHBOARD_RECENT_ITEMS_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val refreshPhase = MutableStateFlow<RefreshPhase>(RefreshPhase.Loading)

    val isRefreshing: StateFlow<Boolean> = refreshPhase
        .map { it is RefreshPhase.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ERR-05 — greska mreze uz postojece lokalne podatke ide kao kratka poruka, ne ceo ekran greske.
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val state: StateFlow<UiState<DashboardUi>> = combine(
        currentUser, aggregates, recentItems, refreshPhase
    ) { user, categoryAggregates, recent, phase ->
        toUiState(user, categoryAggregates, recent, phase)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshPhase.value = RefreshPhase.Loading
            when (val result = itemRepository.refresh()) {
                is Resource.Error -> {
                    refreshPhase.value = RefreshPhase.Failed(result.message)
                    if (aggregates.value.isNotEmpty()) _snackbarMessage.emit(result.message)
                }
                else -> refreshPhase.value = RefreshPhase.Done
            }
        }
    }

    private fun toUiState(
        user: User,
        aggregates: List<CategoryAggregate>,
        recent: List<ItemListRow>,
        phase: RefreshPhase
    ): UiState<DashboardUi> = when {
        aggregates.isNotEmpty() -> UiState.Success(buildDashboardUi(user, aggregates, recent))
        phase is RefreshPhase.Loading -> UiState.Loading
        phase is RefreshPhase.Failed -> UiState.Error(phase.message)
        else -> UiState.Empty
    }

    // BR-011 — efektivna vrednost; BR-009/BR-013 — dok konverzija valuta (tiket 21) ne postoji,
    // predmeti u drugoj valuti od korisnikove se izdvajaju umesto da se pogresno racunaju kao 1:1.
    private fun buildDashboardUi(
        user: User,
        aggregates: List<CategoryAggregate>,
        recent: List<ItemListRow>
    ): DashboardUi {
        val totalItemCount = aggregates.sumOf { it.itemCount }
        val valueByCurrency = aggregates.groupBy { it.currency }
            .mapValues { (_, rows) -> rows.sumOf { it.totalMinor } }
        val totalValueMinor = valueByCurrency[user.currency] ?: 0L
        val hasUnconvertedCurrencies = valueByCurrency.keys.any { it != user.currency }

        val categoryCounts = aggregates.groupBy { it.categoryId to it.categoryName }
            .map { (key, rows) -> CategoryCountUi(key.second, rows.sumOf { it.itemCount }) }
            .sortedByDescending { it.itemCount }

        val recentUi = recent.map {
            RecentItemUi(
                id = it.id,
                name = it.name,
                categoryName = it.categoryName,
                locationName = it.locationName,
                valueFormatted = MoneyFormatter.format(it.effectiveValueMinor(), it.currency)
            )
        }

        return DashboardUi(
            totalItemCount = totalItemCount,
            totalValueFormatted = MoneyFormatter.format(totalValueMinor, user.currency),
            hasUnconvertedCurrencies = hasUnconvertedCurrencies,
            categoryCounts = categoryCounts,
            recentItems = recentUi
        )
    }

    private sealed interface RefreshPhase {
        data object Loading : RefreshPhase
        data object Done : RefreshPhase
        data class Failed(val message: String) : RefreshPhase
    }
}
