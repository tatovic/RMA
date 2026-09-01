package rs.homeinventory.app.presentation.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
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
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.data.local.dao.ItemListRow
import rs.homeinventory.app.data.local.dao.effectiveValueMinor
import rs.homeinventory.app.data.local.prefs.InventoryPreferences
import rs.homeinventory.app.data.local.prefs.WarrantyPreferences
import rs.homeinventory.app.data.remote.mapper.DateMapper
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.domain.model.WarrantyStatus
import rs.homeinventory.app.domain.util.MoneyFormatter
import rs.homeinventory.app.domain.util.WarrantyCalculator
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SEARCH_DEBOUNCE_MS
import rs.homeinventory.app.util.SearchQueryNormalizer
import rs.homeinventory.app.util.UiState
import rs.homeinventory.app.util.WARRANTY_THRESHOLD_DEFAULT_DAYS
import javax.inject.Inject

// SCR-04 — ekran cita iskljucivo iz Room-a, mreza samo puni bazu (tech.md sekcija 5.3, DEP-02).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    authRepository: AuthRepository,
    private val inventoryPreferences: InventoryPreferences,
    private val warrantyPreferences: WarrantyPreferences,
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

    // SCR-05 — padajuce liste kategorija/lokacija za panel filtera (tiket 20).
    val categories: StateFlow<List<CategoryEntity>> = itemRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locations: StateFlow<List<LocationEntity>> = currentUser
        .flatMapLatest { itemRepository.observeLocations(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // BR-009 — valuta prikaza korisnika; oznacava polja za raspon cene u panelu filtera.
    val userCurrency: StateFlow<String?> = currentUser.map { it.currency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Filteri (FR-033 do FR-037) — cuvaju se pojedinacno u SavedStateHandle, isti razlog kao
    // searchQuery: prezivljavaju rotaciju ekrana, ali ne i ponovno pokretanje aplikacije (tiket 20). ----
    val categoryFilterIds: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FILTER_CATEGORY_IDS, "")
    val locationFilterIds: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FILTER_LOCATION_IDS, "")
    val minPriceText: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FILTER_MIN_PRICE, "")
    val maxPriceText: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FILTER_MAX_PRICE, "")
    val purchaseYearText: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FILTER_PURCHASE_YEAR, "")
    private val underWarrantyOnly: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_FILTER_UNDER_WARRANTY, false)
    private val warrantyExpiringSoonOnly: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_FILTER_WARRANTY_EXPIRING_SOON, false)

    fun onCategoryFilterChanged(ids: Set<String>) {
        savedStateHandle[KEY_FILTER_CATEGORY_IDS] = ids.joinToString(",")
    }

    fun onLocationFilterChanged(ids: Set<String>) {
        savedStateHandle[KEY_FILTER_LOCATION_IDS] = ids.joinToString(",")
    }

    fun onMinPriceChanged(text: String) {
        savedStateHandle[KEY_FILTER_MIN_PRICE] = text
    }

    fun onMaxPriceChanged(text: String) {
        savedStateHandle[KEY_FILTER_MAX_PRICE] = text
    }

    fun onPurchaseYearChanged(text: String) {
        savedStateHandle[KEY_FILTER_PURCHASE_YEAR] = text
    }

    fun onUnderWarrantyOnlyChanged(value: Boolean) {
        savedStateHandle[KEY_FILTER_UNDER_WARRANTY] = value
    }

    fun onWarrantyExpiringSoonOnlyChanged(value: Boolean) {
        savedStateHandle[KEY_FILTER_WARRANTY_EXPIRING_SOON] = value
    }

    // Dugme "Ponisti sve filtere" — sortiranje namerno ostaje netaknuto, ono nije filter.
    fun onResetFilters() {
        savedStateHandle[KEY_FILTER_CATEGORY_IDS] = ""
        savedStateHandle[KEY_FILTER_LOCATION_IDS] = ""
        savedStateHandle[KEY_FILTER_MIN_PRICE] = ""
        savedStateHandle[KEY_FILTER_MAX_PRICE] = ""
        savedStateHandle[KEY_FILTER_PURCHASE_YEAR] = ""
        savedStateHandle[KEY_FILTER_UNDER_WARRANTY] = false
        savedStateHandle[KEY_FILTER_WARRANTY_EXPIRING_SOON] = false
    }

    // Tekstualna polja se parsiraju ovde (VR-10/VR-11 stil preko MoneyFormatter) da bi panel i lista
    // delili isto izvedeno stanje — nevazeci/prazan unos se tretira kao "filter nije postavljen".
    val filterState: StateFlow<InventoryFilterState> = combine(
        combine(categoryFilterIds, locationFilterIds, minPriceText, maxPriceText, purchaseYearText) {
            categoryIdsRaw, locationIdsRaw, minRaw, maxRaw, yearRaw ->
            RawTextFilters(categoryIdsRaw, locationIdsRaw, minRaw, maxRaw, yearRaw)
        },
        underWarrantyOnly,
        warrantyExpiringSoonOnly
    ) { raw, underWarranty, expiringSoon ->
        InventoryFilterState(
            categoryIds = raw.categoryIdsRaw.toIdSet(),
            locationIds = raw.locationIdsRaw.toIdSet(),
            minPriceMinor = MoneyFormatter.parseToMinor(raw.minPriceRaw),
            maxPriceMinor = MoneyFormatter.parseToMinor(raw.maxPriceRaw),
            purchaseYear = raw.purchaseYearRaw.trim().toIntOrNull(),
            underWarrantyOnly = underWarranty,
            warrantyExpiringSoonOnly = expiringSoon
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryFilterState())

    // ---- Sortiranje (FR-038) — cuva se u DataStore, prezivljava i rotaciju i ponovno pokretanje (tiket 20). ----
    val sortMode: StateFlow<InventorySortMode> = inventoryPreferences.sortMode
        .map(InventorySortMode::fromStorageKey)
        .stateIn(viewModelScope, SharingStarted.Eagerly, InventorySortMode.DEFAULT)

    fun onSortModeSelected(mode: InventorySortMode) {
        viewModelScope.launch { inventoryPreferences.saveSortMode(mode.name) }
    }

    // ---- Prag garancije (FR-051/FR-052) — cuva se u DataStore, koristi ga i filter ovde i status na
    // stavci liste (tiket 22). ----
    val warrantyThreshold: StateFlow<Int> = warrantyPreferences.thresholdDays
        .stateIn(viewModelScope, SharingStarted.Eagerly, WARRANTY_THRESHOLD_DEFAULT_DAYS)

    // FR-033 do FR-038 — filteri i sortiranje se kombinuju sa pretragom logickim I (tiket 20).
    private val filteredResults: StateFlow<List<ItemListRow>> = combine(
        searchResults, filterState, sortMode, currentUser, warrantyThreshold
    ) { rows, filters, sort, user, threshold ->
        applyFiltersAndSort(rows, filters, sort, user.currency, threshold)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val refreshPhase = MutableStateFlow<RefreshPhase>(RefreshPhase.Loading)

    val isRefreshing: StateFlow<Boolean> = refreshPhase
        .map { it is RefreshPhase.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ERR-05 — greska mreze uz postojece lokalne podatke ide kao kratka poruka, ne ceo ekran greske.
    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val state: StateFlow<UiState<List<InventoryItemUi>>> = combine(
        items, filteredResults, refreshPhase, warrantyThreshold
    ) { allRows, filteredRows, phase, threshold ->
        toUiState(allRows, filteredRows, phase, threshold)
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
    // pretraga/filteri bez pogotka ne smeju da izgledaju kao da inventar nikad nije ucitan (Loading/Error).
    private fun toUiState(
        allRows: List<ItemListRow>,
        filteredRows: List<ItemListRow>,
        phase: RefreshPhase,
        thresholdDays: Int
    ): UiState<List<InventoryItemUi>> = when {
        filteredRows.isNotEmpty() -> UiState.Success(filteredRows.map { toItemUi(it, thresholdDays) })
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

    // FR-055/BR-010 — status garancije stavke, izveden istim pravilom kao svuda (tiket 22).
    private fun toItemUi(row: ItemListRow, thresholdDays: Int): InventoryItemUi = InventoryItemUi(
        id = row.id,
        name = row.name,
        categoryName = row.categoryName,
        locationName = row.locationName,
        priceFormatted = MoneyFormatter.format(row.effectiveValueMinor(), row.currency),
        imagePath = row.imagePath,
        categoryIconKey = row.categoryIconKey,
        warrantyStatus = WarrantyCalculator.status(DateMapper.parseLocalDate(row.warrantyExpirationDate), thresholdDays)
    )

    // FR-033 do FR-038 — filtriranje i sortiranje nad vec pretrazenim redovima (tiket 20).
    // BR-009 — poredjenje cena ide iskljucivo preko valute prikaza korisnika. Dok prava konverzija
    // (kursna lista, tiket 21) ne postoji, predmeti u drugoj valuti se ne mogu korektno uporediti:
    // filter po rasponu cene ih izostavlja (isti duh kao BR-013/tiket 13), a sortiranje po ceni ih
    // grupise na kraj liste umesto da im sirovi brojevi budu pomesani sa konvertovanim vrednostima.
    private fun applyFiltersAndSort(
        rows: List<ItemListRow>,
        filters: InventoryFilterState,
        sort: InventorySortMode,
        displayCurrency: String,
        thresholdDays: Int
    ): List<ItemListRow> {
        val today = LocalDate.now()
        val filtered = rows.filter { row ->
            (filters.categoryIds.isEmpty() || row.categoryId in filters.categoryIds) &&
                (filters.locationIds.isEmpty() || row.locationId in filters.locationIds) &&
                matchesPriceRange(row, filters, displayCurrency) &&
                matchesPurchaseYear(row, filters.purchaseYear) &&
                matchesWarranty(row, filters, today, thresholdDays)
        }
        return sortRows(filtered, sort, displayCurrency)
    }

    private fun matchesPriceRange(row: ItemListRow, filters: InventoryFilterState, displayCurrency: String): Boolean {
        if (filters.minPriceMinor == null && filters.maxPriceMinor == null) return true
        if (row.currency != displayCurrency) return false // BR-009 — bez konverzije ne moze da se uporedi
        val value = row.effectiveValueMinor()
        return (filters.minPriceMinor == null || value >= filters.minPriceMinor) &&
            (filters.maxPriceMinor == null || value <= filters.maxPriceMinor)
    }

    private fun matchesPurchaseYear(row: ItemListRow, year: Int?): Boolean {
        if (year == null) return true
        return row.purchaseDate?.take(4)?.toIntOrNull() == year
    }

    // "Pod garancijom" = status AKTIVNA ili USKORO_ISTICE (D >= T). "Uskoro istice" je uzi podskup toga,
    // do korisnikovog praga (FR-051) — racuna se preko WarrantyCalculator, isto pravilo svuda (BR-010, tiket 22).
    private fun matchesWarranty(row: ItemListRow, filters: InventoryFilterState, today: LocalDate, thresholdDays: Int): Boolean {
        if (!filters.underWarrantyOnly && !filters.warrantyExpiringSoonOnly) return true
        val expiration = DateMapper.parseLocalDate(row.warrantyExpirationDate) ?: return false
        val status = WarrantyCalculator.status(expiration, thresholdDays, today)
        val underWarranty = status == WarrantyStatus.AKTIVNA || status == WarrantyStatus.USKORO_ISTICE
        val expiringSoon = status == WarrantyStatus.USKORO_ISTICE
        return (!filters.underWarrantyOnly || underWarranty) && (!filters.warrantyExpiringSoonOnly || expiringSoon)
    }

    private fun sortRows(rows: List<ItemListRow>, sort: InventorySortMode, displayCurrency: String): List<ItemListRow> {
        val locale = Locale("sr", "RS")
        return when (sort) {
            InventorySortMode.NAME_ASC -> rows.sortedBy { it.name.lowercase(locale) }
            InventorySortMode.NAME_DESC -> rows.sortedByDescending { it.name.lowercase(locale) }
            InventorySortMode.PRICE_ASC -> rows.sortedWith(priceComparator(displayCurrency))
            InventorySortMode.PRICE_DESC -> rows.sortedWith(priceComparator(displayCurrency).reversed())
            InventorySortMode.NEWEST -> rows.sortedByDescending { it.createdAt }
            InventorySortMode.OLDEST -> rows.sortedBy { it.createdAt }
        }
    }

    // BR-011 — poredjenje ide preko efektivne vrednosti; stavke van valute prikaza idu na kraj (BR-009, vidi gore).
    private fun priceComparator(displayCurrency: String): Comparator<ItemListRow> =
        compareBy<ItemListRow> { if (it.currency == displayCurrency) 0 else 1 }
            .thenBy { it.effectiveValueMinor() }

    private fun String.toIdSet(): Set<String> = split(",").filter { it.isNotBlank() }.toSet()

    private data class RawTextFilters(
        val categoryIdsRaw: String,
        val locationIdsRaw: String,
        val minPriceRaw: String,
        val maxPriceRaw: String,
        val purchaseYearRaw: String
    )

    private sealed interface RefreshPhase {
        data object Loading : RefreshPhase
        data object Done : RefreshPhase
        data class Failed(val message: String) : RefreshPhase
    }

    private companion object {
        const val KEY_SEARCH_QUERY = "search_query"
        const val KEY_FILTER_CATEGORY_IDS = "filter_category_ids"
        const val KEY_FILTER_LOCATION_IDS = "filter_location_ids"
        const val KEY_FILTER_MIN_PRICE = "filter_min_price"
        const val KEY_FILTER_MAX_PRICE = "filter_max_price"
        const val KEY_FILTER_PURCHASE_YEAR = "filter_purchase_year"
        const val KEY_FILTER_UNDER_WARRANTY = "filter_under_warranty"
        const val KEY_FILTER_WARRANTY_EXPIRING_SOON = "filter_warranty_expiring_soon"
    }
}
