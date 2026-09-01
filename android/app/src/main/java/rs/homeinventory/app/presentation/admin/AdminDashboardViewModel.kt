package rs.homeinventory.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.remote.dto.AdminStatsDto
import rs.homeinventory.app.data.repository.AdminRepository
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.UiState
import javax.inject.Inject

// SCR-11 — pregled sistema. Za razliku od Dashboard/Statistika, brojevi su globalni pa ekran zove
// server direktno umesto Room-a (AdminRepository, tiket 25).
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AdminStatsUi>>(UiState.Loading)
    val state: StateFlow<UiState<AdminStatsUi>> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load(showLoading = true)
    }

    fun refresh() = load(showLoading = false)

    private fun load(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.value = UiState.Loading else _isRefreshing.value = true
            when (val result = adminRepository.getStats()) {
                is Resource.Success -> _state.value = UiState.Success(result.data.toUi())
                is Resource.Error -> _state.value = UiState.Error(result.message)
                Resource.Loading -> Unit
            }
            _isRefreshing.value = false
        }
    }

    private fun AdminStatsDto.toUi() = AdminStatsUi(
        registeredUsers = registeredUsers,
        activeUsers = activeUsers,
        deactivatedUsers = deactivatedUsers,
        totalItems = totalItems,
        totalCategories = totalCategories
    )
}
