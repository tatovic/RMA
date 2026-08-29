package rs.homeinventory.app.presentation.inventory

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
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.effectiveValueMinor
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.domain.util.MoneyFormatter
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-04 — ekran cita iskljucivo iz Room-a, mreza samo puni bazu (tech.md sekcija 5.3, DEP-02).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val currentUser = authRepository.currentUser.filterNotNull()

    // SharingStarted.Eagerly — mora odrazavati pravo stanje lokalne baze nezavisno od toga
    // da li Fragment vec sluša, jer refresh() proverava items.value pre nego sto UI pretplati state.
    private val items: StateFlow<List<ItemListRow>> = currentUser
        .flatMapLatest { itemRepository.observeAllItems(it.id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val refreshPhase = MutableStateFlow<RefreshPhase>(RefreshPhase.Loading)

    val isRefreshing: StateFlow<Boolean> = refreshPhase
        .map { it is RefreshPhase.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ERR-05 — greska mreze uz postojece lokalne podatke ide kao kratka poruka, ne ceo ekran greske.
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val state: StateFlow<UiState<List<InventoryItemUi>>> = combine(
        items, refreshPhase
    ) { rows, phase ->
        toUiState(rows, phase)
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

    private fun toUiState(
        rows: List<ItemListRow>,
        phase: RefreshPhase
    ): UiState<List<InventoryItemUi>> = when {
        rows.isNotEmpty() -> UiState.Success(rows.map(::toItemUi))
        phase is RefreshPhase.Loading -> UiState.Loading
        phase is RefreshPhase.Failed -> UiState.Error(phase.message)
        else -> UiState.Empty
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
}
