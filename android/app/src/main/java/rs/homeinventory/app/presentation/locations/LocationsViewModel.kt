package rs.homeinventory.app.presentation.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.local.dao.LocationWithCount
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.data.repository.ItemRepository
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.LocationValidator
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-10 — spisak lokacija, sa dodavanjem/izmenom/brisanjem (tiket 17). Ekran cita iskljucivo iz
// Room-a; kategorije/lokacije/predmeti se povlace preko drugih ekrana (Dashboard/Inventar) na startu.
// BR-017 Error stanje ovde je odbrambeno (citanje iz Room-a normalno ne baca) — pokriva NFR-11 ako
// lokalna baza ipak zapne, sa dugmetom koji ponovo pokrece pretplatu na Room Flow (retry()).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    authRepository: AuthRepository,
    private val errorMessageProvider: ErrorMessageProvider
) : ViewModel() {

    private val currentUser = authRepository.currentUser.filterNotNull()
    private val retryTrigger = MutableStateFlow(0)

    val state: StateFlow<UiState<List<LocationUi>>> = combine(currentUser, retryTrigger) { user, _ -> user }
        .flatMapLatest { itemRepository.observeLocationsWithCount(it.id) }
        .map { rows -> if (rows.isEmpty()) UiState.Empty else UiState.Success(rows.map(::toUi)) }
        .catch { emit(UiState.Error(errorMessageProvider.message(ErrorCode.UNKNOWN))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() {
        retryTrigger.value++
    }

    private val _fieldErrors = MutableStateFlow(LocationValidator.Errors())
    val fieldErrors: StateFlow<LocationValidator.Errors> = _fieldErrors.asStateFlow()

    private val _formState = MutableStateFlow<Resource<Unit>?>(null)
    val formState: StateFlow<Resource<Unit>?> = _formState.asStateFlow()

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState.asStateFlow()

    // Cuvanje (dodavanje/izmena) je mreza-prvo — VR-19 (jedinstven naziv) je server konacno proverava.
    fun save(existingId: String?, input: LocationValidator.Input) {
        if (_formState.value is Resource.Loading) return

        val (errors, parsed) = LocationValidator.validate(input)
        _fieldErrors.value = errors
        if (parsed == null) return

        // Zastita se postavlja SINHRONO, pre launch-a — `launch` samo zakazuje korutinu, pa dva
        // brza dodira oba prodju kroz proveru iznad (tiket 28, nalaz 09).
        _formState.value = Resource.Loading

        viewModelScope.launch {
            _formState.value = if (existingId == null) {
                itemRepository.createLocation(parsed.name, parsed.description)
            } else {
                itemRepository.updateLocation(existingId, parsed.name, parsed.description)
            }
        }
    }

    // BR-014 — poziva se tek posto je ekran vec proverio da lokacija nema predmeta.
    fun delete(id: String) {
        if (_deleteState.value is Resource.Loading) return
        // Zastita se postavlja SINHRONO, pre launch-a — `launch` samo zakazuje korutinu, pa dva
        // brza dodira oba prodju kroz proveru iznad (tiket 28, nalaz 09).
        _deleteState.value = Resource.Loading

        viewModelScope.launch {
            _deleteState.value = itemRepository.deleteLocation(id)
        }
    }

    // Priprema forme za ponovno otvaranje (nov ili drugi unos) bez ostataka prethodnog pokusaja.
    fun resetForm() {
        _fieldErrors.value = LocationValidator.Errors()
        _formState.value = null
    }

    fun consumeDeleteState() {
        _deleteState.value = null
    }

    private fun toUi(row: LocationWithCount): LocationUi =
        LocationUi(id = row.id, name = row.name, description = row.description, itemCount = row.itemCount)
}
