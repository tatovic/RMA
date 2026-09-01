package rs.homeinventory.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.data.remote.dto.AdminUserDto
import rs.homeinventory.app.data.repository.AdminRepository
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-12 — spisak korisnika sa promenom statusa (tiket 25). Server ostaje konacna potvrda
// self-deaktivacije i drugih gresaka (BR-004) — ekran samo sprecava ociglednu gresku unapred.
@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AdminUserUi>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminUserUi>>> = _state.asStateFlow()

    private val _statusUpdateError = MutableStateFlow<String?>(null)
    val statusUpdateError: StateFlow<String?> = _statusUpdateError.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    fun updateStatus(id: String, isActive: Boolean) {
        viewModelScope.launch {
            when (val result = adminRepository.updateUserStatus(id, isActive)) {
                is Resource.Success -> load()
                is Resource.Error -> _statusUpdateError.value = result.message
                Resource.Loading -> Unit
            }
        }
    }

    fun consumeStatusUpdateError() {
        _statusUpdateError.value = null
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val selfId = authRepository.currentUser.first()?.id
            when (val result = adminRepository.getUsers()) {
                is Resource.Success -> _state.value =
                    if (result.data.isEmpty()) UiState.Empty
                    else UiState.Success(result.data.map { it.toUi(isSelf = it.id == selfId) })
                is Resource.Error -> _state.value = UiState.Error(result.message)
                Resource.Loading -> Unit
            }
        }
    }

    private fun AdminUserDto.toUi(isSelf: Boolean) = AdminUserUi(
        id = id,
        name = name,
        email = email,
        role = if (role == UserRole.ADMIN.name) UserRole.ADMIN else UserRole.USER,
        isActive = isActive,
        itemCount = itemCount,
        isSelf = isSelf
    )
}
