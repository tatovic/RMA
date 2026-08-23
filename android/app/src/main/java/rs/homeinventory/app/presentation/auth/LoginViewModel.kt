package rs.homeinventory.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.util.Resource
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<User>?>(null)
    val state: StateFlow<Resource<User>?> = _state.asStateFlow()

    fun login(email: String, password: String) {
        // Sprecava dupli zahtev dok je jedan vec u toku.
        if (_state.value is Resource.Loading) return
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = authRepository.login(email.trim(), password)
        }
    }
}
