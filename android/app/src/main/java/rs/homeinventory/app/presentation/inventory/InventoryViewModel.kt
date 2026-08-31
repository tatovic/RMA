package rs.homeinventory.app.presentation.inventory

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.effectiveValueMinor
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.domain.util.MoneyFormatter
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SEARCH_DEBOUNCE_MS
import rs.homeinventory.app.util.SearchQueryNormalizer
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-04 — ekran cita iskljucivo iz Room-a, mreza samo puni bazu (tech.md sekcija 5.3, DEP-02).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val currentUser = authRepository.currentUser.filterNotNull()

    // SharingStarted.Eagerly — mora odrazavati pravo stanje lokalne baze nezavisno od toga
    // da li Fragment vec sluša, jer refresh() proverava items.value pre nego sto UI pretplati state.
    private val items: StateFlow<List<ItemListRow>> = currentUser
        .flatMapLatest { itemRepository.observeAllItems(it.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // FR-031/NFR-04 — cuva se u SavedStateHandle da uneti pojam prezivi rotaciju ekrana (tiket 19).
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    // FR-032 — 300ms zadrska pre nego sto se upit normalizuje i posalje u bazu (tech.md 8.5).
    // Prazan upit (pocetno stanje ili brzo brisanje pretrage) se ne odlaze - lista se odmah vraca.
    private val normalizedQuery = searchQuery
        .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()
        .map(SearchQueryNormalizer::normalize)

    // FR-031 — pretraga po sest polja se izvrsava nad Room-om (ItemDao.search), upit stize vec
    // normalizovan iz Kotlina, a ne oslanja se samo na sirovo SQL poredjenje (tiket 19).
    private val searchResults: StateFlow<List<ItemListRow>> = combine(
        currentUser, normalizedQuery
    ) { user, query -> user.id to query }
        .flatMapLatest { (userId, query) -> itemRepository.searchItems(userId, query) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val refreshPhase = MutableStateFlow<RefreshPhase>(RefreshPhase.Loading)

    val isRefreshing: StateFlow<Boolean> = refreshPhase
        .map { it is RefreshPhase.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ERR-05 — greska mreze uz postojece lokalne podatke ide kao kratka poruka, ne ceo ekran greske.
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val state: StateFlow<UiState<List<InventoryItemUi>>> = combine(
        items, searchResults, refreshPhase
    ) { allRows, filteredRows, phase ->
        toUiState(allRows, filteredRows, phase)
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
                    if (items.value.isNotEmpty()) _snackbarMessage.emit(result.message)
                }
                else -> refreshPhase.value = RefreshPhase.Done
            }
        }
    }

    // Prisustvo lokalnih podataka (allRows) se proverava odvojeno od filtriranih rezultata (filteredRows) -
    // pretraga bez pogotka ne sme da izgleda kao da inventar nikad nije ucitan (Loading/Error).
    private fun toUiState(
        allRows: List<ItemListRow>,
        filteredRows: List<ItemListRow>,
        phase: RefreshPhase
    ): UiState<List<InventoryItemUi>> = when {
        filteredRows.isNotEmpty() -> UiState.Success(filteredRows.map(::toItemUi))
        allRows.isNotEmpty() -> UiState.Empty
        phase is RefreshPhase.Loading -> UiState.Loading
        phase is RefreshPhase.Failed -> UiState.Error(phase.message)
        else -> UiState.Empty
    }

    // FR-027 — opoziv brisanja predmeta u roku od pet sekundi.
    fun undoDelete(id: String) {
        viewModelScope.launch { itemRepository.undoDelete(id) }
    }

    // FR-086 — poziva se kad opoziv vise nije ponudjen (snackbar istekao/odbacen), da fotografija
    // trajno obrisanog predmeta ne ostane na disku.
    fun finalizeDeletedItemPhoto(id: String) {
        viewModelScope.launch { itemRepository.finalizeDeletedItemPhoto(id) }
    }

    private fun toItemUi(row: ItemListRow): InventoryItemUi = InventoryItemUi(
        id = row.id,
        name = row.name,
        categoryName = row.categoryName,
        locationName = row.locationName,
        priceFormatted = MoneyFormatter.format(row.effectiveValueMinor(), row.currency),
        imagePath = row.imagePath,
        categoryIconKey = row.categoryIconKey
    )

    private sealed interface RefreshPhase {
        data object Loading : RefreshPhase
        data object Done : RefreshPhase
        data class Failed(val message: String) : RefreshPhase
    }

    private companion object {
        const val KEY_SEARCH_QUERY = "search_query"
    }
}
