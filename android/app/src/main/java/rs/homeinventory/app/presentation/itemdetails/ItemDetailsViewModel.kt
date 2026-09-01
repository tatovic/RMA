package rs.homeinventory.app.presentation.itemdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.local.dao.ItemDetailsRow
import rs.homeinventory.app.data.local.dao.effectiveValueMinor
import rs.homeinventory.app.data.local.prefs.WarrantyPreferences
import rs.homeinventory.app.data.remote.mapper.DateMapper
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.CurrencyRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.domain.model.WarrantyStatus
import rs.homeinventory.app.domain.util.CurrencyConverter
import rs.homeinventory.app.domain.util.MoneyFormatter
import rs.homeinventory.app.domain.util.WarrantyCalculator
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// SCR-07 — ekran prima samo itemId (BR-007) i cita detalje direktno iz Room-a preko join upita.
@HiltViewModel
class ItemDetailsViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val currencyRepository: CurrencyRepository,
    private val authRepository: AuthRepository,
    private val warrantyPreferences: WarrantyPreferences,
    private val errorMessageProvider: ErrorMessageProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val itemId: String = checkNotNull(savedStateHandle["itemId"])

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy.")

    // US-15/FR-063 — kursevi za konverziju prikazanog iznosa (BR-013 ako neki kurs nedostaje).
    private val rates = MutableStateFlow<Map<String, Double>>(emptyMap())

    init {
        viewModelScope.launch {
            val result = currencyRepository.getRates()
            if (result is Resource.Success) rates.value = result.data
        }
    }

    // Nepostojeci ili obrisan identifikator (BR-007) prikazuje poruku umesto rusenja aplikacije.
    val state: StateFlow<UiState<ItemDetailsUi>> = combine(
        itemRepository.observeItemDetails(itemId),
        authRepository.currentUser.filterNotNull(),
        rates,
        warrantyPreferences.thresholdDays
    ) { row, user, currentRates, thresholdDays ->
        row?.let { UiState.Success(toUi(it, user.currency, currentRates, thresholdDays)) }
            ?: UiState.Error(errorMessageProvider.message(ErrorCode.NOT_FOUND))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState.asStateFlow()

    // Brisanje (FR-025/FR-026) — soft delete lokalno pa odmah pokusaj slanja serveru; puna sinhronizacija je tiket 26.
    fun delete() {
        if (_deleteState.value is Resource.Loading) return
        viewModelScope.launch {
            _deleteState.value = Resource.Loading
            itemRepository.deleteItem(itemId)
            _deleteState.value = Resource.Success(Unit)
        }
    }

    // US-15 — predmet cija se valuta razlikuje od valute prikaza pokazuje i preracunati iznos;
    // BR-013 — ako kurs nedostaje, izdvaja se poruka umesto pogresnog broja.
    private fun toUi(
        row: ItemDetailsRow,
        displayCurrency: String,
        rates: Map<String, Double>,
        thresholdDays: Int
    ): ItemDetailsUi {
        val showsConversion = row.currency != displayCurrency
        val convertedValueMinor = if (showsConversion) {
            CurrencyConverter.convert(row.effectiveValueMinor(), row.currency, displayCurrency, rates)
        } else {
            null
        }

        // BR-010 — status i preostali dani, uvek izvedeni iz datuma isteka, nikad sacuvani (tiket 22).
        val warrantyExpiration = DateMapper.parseLocalDate(row.warrantyExpirationDate)
        val warrantyStatus = WarrantyCalculator.status(warrantyExpiration, thresholdDays)
        val warrantyDaysRemaining = if (warrantyExpiration != null && warrantyStatus != WarrantyStatus.ISTEKLA) {
            WarrantyCalculator.daysRemaining(warrantyExpiration).toInt()
        } else {
            null
        }

        return ItemDetailsUi(
            name = row.name,
            categoryName = row.categoryName,
            locationName = row.locationName,
            description = row.description,
            manufacturer = row.manufacturer,
            model = row.model,
            serialNumber = row.serialNumber,
            quantity = row.quantity,
            purchasePriceFormatted = row.purchasePrice?.let { MoneyFormatter.format(it, row.currency) },
            estimatedValueFormatted = row.estimatedValue?.let { MoneyFormatter.format(it, row.currency) },
            purchaseDateFormatted = DateMapper.parseLocalDate(row.purchaseDate)?.format(dateFormatter),
            warrantyExpirationDateFormatted = DateMapper.parseLocalDate(row.warrantyExpirationDate)?.format(dateFormatter),
            seller = row.seller,
            notes = row.notes,
            imagePath = row.imagePath,
            convertedValueFormatted = convertedValueMinor?.let { MoneyFormatter.format(it, displayCurrency) },
            convertedValueUnavailable = showsConversion && convertedValueMinor == null,
            warrantyStatus = warrantyStatus,
            warrantyDaysRemaining = warrantyDaysRemaining
        )
    }
}
