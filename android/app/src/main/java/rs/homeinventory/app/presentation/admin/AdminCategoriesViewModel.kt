package rs.homeinventory.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rs.homeinventory.app.R
import rs.homeinventory.app.data.remote.dto.CategoryDto
import rs.homeinventory.app.data.repository.AdminRepository
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-13 — dodavanje/preimenovanje/brisanje globalnih kategorija (tiket 25). Puni naziv obrazac
// (opis/ikonica/redosled) se ne menja ovde — samo naziv; AdminRepository cuva ostatak nepromenjen.
@HiltViewModel
class AdminCategoriesViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AdminCategoryUi>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminCategoryUi>>> = _state.asStateFlow()

    // @StringRes — resurs poruke o gresci u nazivu.
    private val _fieldError = MutableStateFlow<Int?>(null)
    val fieldError: StateFlow<Int?> = _fieldError.asStateFlow()

    private val _formState = MutableStateFlow<Resource<Unit>?>(null)
    val formState: StateFlow<Resource<Unit>?> = _formState.asStateFlow()

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState.asStateFlow()

    // Puni DTO-ovi (opis/ikonica/redosled) za rename — AdminCategoryUi nosi samo ono sto se prikazuje.
    // Uvek popunjen pre nego sto ekran omoguci izmenu, jer existingId dolazi iz vec ucitane liste.
    private var categoriesById: Map<String, CategoryDto> = emptyMap()

    init {
        load()
    }

    fun refresh() = load()

    // Isti obrazac kao LocationsViewModel.save (tiket 17): mreza-prvo, VR proverava samo prazninu/dužinu.
    fun save(existingId: String?, name: String) {
        if (_formState.value is Resource.Loading) return

        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 60) {
            _fieldError.value = R.string.error_vr_category_name
            return
        }
        _fieldError.value = null

        viewModelScope.launch {
            _formState.value = Resource.Loading
            val result = if (existingId == null) {
                adminRepository.createCategory(trimmed)
            } else {
                adminRepository.renameCategory(checkNotNull(categoriesById[existingId]), trimmed)
            }
            _formState.value = result
            if (result is Resource.Success) load()
        }
    }

    fun delete(id: String) {
        if (_deleteState.value is Resource.Loading) return
        viewModelScope.launch {
            _deleteState.value = Resource.Loading
            val result = adminRepository.deleteCategory(id)
            _deleteState.value = result
            if (result is Resource.Success) load()
        }
    }

    fun resetForm() {
        _fieldError.value = null
        _formState.value = null
    }

    fun consumeDeleteState() {
        _deleteState.value = null
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = adminRepository.getCategories()) {
                is Resource.Success -> {
                    categoriesById = result.data.associateBy { it.id }
                    _state.value = if (result.data.isEmpty()) UiState.Empty
                    else UiState.Success(result.data.map { it.toUi() }.sortedBy { it.name })
                }
                is Resource.Error -> _state.value = UiState.Error(result.message)
                Resource.Loading -> Unit
            }
        }
    }

    private fun CategoryDto.toUi() = AdminCategoryUi(id = id, name = name, itemCount = itemCount)
}
