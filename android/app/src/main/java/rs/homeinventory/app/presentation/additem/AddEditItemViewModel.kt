package rs.homeinventory.app.presentation.additem

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.data.remote.mapper.DateMapper
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.ItemValidator
import rs.homeinventory.app.util.PhotoStorage
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// SCR-06 — ista forma sluzi za dodavanje i izmenu (itemId argument iz Safe Args, null = dodavanje).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddEditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val authRepository: AuthRepository,
    private val photoStorage: PhotoStorage,
    private val errorMessageProvider: ErrorMessageProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String? = savedStateHandle["itemId"]
    val isEditMode: Boolean get() = itemId != null

    private val currentUser = authRepository.currentUser.filterNotNull()

    val categories: StateFlow<List<CategoryEntity>> = itemRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locations: StateFlow<List<LocationEntity>> = currentUser
        .flatMapLatest { itemRepository.observeLocations(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _fieldErrors = MutableStateFlow(ItemValidator.Errors())
    val fieldErrors: StateFlow<ItemValidator.Errors> = _fieldErrors.asStateFlow()

    private val _saveState = MutableStateFlow<Resource<String>?>(null)
    val saveState: StateFlow<Resource<String>?> = _saveState.asStateFlow()

    // Novoodabrana fotografija u ovoj sesiji uredjivanja (FR-081/FR-082/FR-083) — jos nije upisana na
    // predmet, sve dok korisnik ne sacuva formu. Null znaci da fotografija nije menjana.
    private val _pendingImagePath = MutableStateFlow<String?>(null)
    val pendingImagePath: StateFlow<String?> = _pendingImagePath.asStateFlow()

    // NFR-04 (tiket 27) — polja forme koja nisu obican tekst (pa ih Android sam ne cuva kroz
    // rotaciju preko view state-a) zive ovde, u ViewModel-u koji rotaciju preživljava nepromenjen;
    // Fragment ih cita/pise direktno umesto da ih drzi kao sopstvena polja koja rotacija brise.
    var selectedCategoryId: String? = null
    var selectedLocationId: String? = null
    var selectedPurchaseDate: LocalDate? = null
    var selectedWarrantyDate: LocalDate? = null
    var existingImagePath: String? = null
    var pendingCameraUri: Uri? = null
    var formSnapshot: FormSnapshot? = null

    // Sprecava da se forma ponovo popuni iznad korisnikovog unosa posle rotacije — populateForm()
    // sme da se pozove tacno jednom po otvaranju ekrana, ne pri svakom onViewCreated.
    var formPopulated: Boolean = false

    // BR-017 — ucitavanje pocetnih podataka (edit rezim cita postojeci predmet iz Room-a) sada je
    // deo ViewModel-a, pokrenuto tacno jednom u init{}, da rotacija ne ponavlja poziv niti gubi stanje.
    private val _initialDataState = MutableStateFlow<UiState<InitialData>>(UiState.Loading)
    val initialDataState: StateFlow<UiState<InitialData>> = _initialDataState.asStateFlow()

    init {
        loadInitialData()
    }

    fun retryLoadInitialData() = loadInitialData()

    private fun loadInitialData() {
        viewModelScope.launch {
            _initialDataState.value = UiState.Loading
            val result = runCatching {
                val user = currentUser.first()
                val existing = itemId?.let { itemRepository.getItem(it) }
                InitialData(existingItem = existing, defaultCurrency = user.currency)
            }
            _initialDataState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(errorMessageProvider.message(ErrorCode.UNKNOWN)) }
            )
        }
    }

    // Korisnik je izabrao novu fotografiju (kamera ili galerija); odmah se smanjuje, kompresuje i
    // kopira u privatni prostor aplikacije. Zamena unutar iste sesije brise prethodno odabrani fajl
    // da se ne gomilaju (isti duh kao FR-086).
    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val newFileName = withContext(Dispatchers.IO) { photoStorage.save(uri) } ?: return@launch
            val previousPending = _pendingImagePath.value
            _pendingImagePath.value = newFileName
            if (previousPending != null) withContext(Dispatchers.IO) { photoStorage.delete(previousPending) }
        }
    }

    // Napustanje forme bez cuvanja (odbacivanje izmena) - novoodabrana fotografija ne sme ostati siroce na disku.
    fun discardPendingPhoto() {
        val pending = _pendingImagePath.value ?: return
        _pendingImagePath.value = null
        viewModelScope.launch(Dispatchers.IO) { photoStorage.delete(pending) }
    }

    // FR-081 — privremeni fajl u koji aplikacija kamere upisuje snimak (deljen preko FileProvider-a).
    fun createCaptureUri(): Uri = photoStorage.createCaptureUri()

    fun save(input: ItemValidator.Input) {
        if (_saveState.value is Resource.Loading) return

        val (errors, parsed) = ItemValidator.validate(input)
        _fieldErrors.value = errors
        if (parsed == null) return

        // Zastita se postavlja SINHRONO, pre launch-a. `launch` samo zakazuje korutinu, pa su dva
        // dodira u istom frame-u oba videla `null` i oba prosla dalje — u rezimu dodavanja bi svaki
        // izmislio svoj UUID i nastala bi dva predmeta (tiket 28, nalaz 09).
        _saveState.value = Resource.Loading

        viewModelScope.launch {
            val user = currentUser.first()
            val existing = itemId?.let { itemRepository.getItem(it) }
            val now = System.currentTimeMillis()
            val isCreate = existing == null || existing.syncStatus == SyncStatus.PENDING_CREATE
            val id = itemId ?: UUID.randomUUID().toString() // FR-029 — UUID v4 generise klijent.

            // FR-083 — nova fotografija (ako je odabrana) zamenjuje staru; stara se brise da se ne gomila (FR-086 duh).
            val pendingImage = _pendingImagePath.value
            val newImagePath = pendingImage ?: existing?.imagePath
            if (pendingImage != null && existing?.imagePath != null) {
                withContext(Dispatchers.IO) { photoStorage.delete(existing.imagePath) }
            }

            val entity = InventoryItemEntity(
                id = id,
                userId = user.id,
                name = parsed.name,
                description = parsed.description,
                categoryId = parsed.categoryId,
                locationId = parsed.locationId,
                manufacturer = parsed.manufacturer,
                model = parsed.model,
                serialNumber = parsed.serialNumber,
                quantity = parsed.quantity,
                purchasePrice = parsed.purchasePrice,
                estimatedValue = parsed.estimatedValue,
                currency = parsed.currency,
                purchaseDate = DateMapper.formatLocalDate(parsed.purchaseDate),
                warrantyExpirationDate = DateMapper.formatLocalDate(parsed.warrantyExpirationDate),
                seller = parsed.seller,
                notes = parsed.notes,
                createdAt = existing?.createdAt ?: now,
                // DB-RULE-04 — `updatedAt` je verzija koju je server poslednju izdao za ovaj red, ne
                // vreme lokalne izmene. Pri izmeni se prenosi nepromenjena, da bi je server prepoznao
                // kao osnovu na koju se izmena naslanja. Sat na telefonu ovde nema sta da trazi:
                // zaostao sat je pravio starije `updatedAt` od serverskog i izmena je zavrsavala kao
                // konflikt cije je "resenje" bilo tiho brisanje korisnikovog unosa (tiket 28,
                // blokirajuci nalaz 03). Kreiranje i dalje koristi `now` — server `updatedAt` pri
                // POST-u ignorise (nema ga u createItemSchema, zod ga odbacuje), pa tu ne moze da smeta.
                updatedAt = existing?.updatedAt ?: now,
                deletedAt = existing?.deletedAt,
                imagePath = newImagePath,
                syncStatus = if (isCreate) SyncStatus.PENDING_CREATE else SyncStatus.PENDING_UPDATE
            )

            itemRepository.saveItem(entity)
            _pendingImagePath.value = null
            _saveState.value = Resource.Success(id)
        }
    }

    data class InitialData(val existingItem: InventoryItemEntity?, val defaultCurrency: String)

    // Snimak forme odmah posle popunjavanja — Fragment ga poredi sa trenutnim stanjem pri napustanju
    // ekrana (BR-008 duh: potvrda pre odbacivanja nesacuvanih izmena). Zivi ovde da preživi rotaciju.
    data class FormSnapshot(
        val name: String,
        val categoryId: String?,
        val locationId: String?,
        val description: String,
        val manufacturer: String,
        val model: String,
        val serialNumber: String,
        val quantity: String,
        val purchasePrice: String,
        val estimatedValue: String,
        val currency: String,
        val purchaseDate: LocalDate?,
        val warrantyExpirationDate: LocalDate?,
        val seller: String,
        val notes: String,
        val photoChanged: Boolean
    )
}
