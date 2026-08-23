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
import rs.homeinventory.app.util.AuthValidator
import rs.homeinventory.app.util.Resource
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _fieldErrors = MutableStateFlow(AuthValidator.RegisterErrors())
    val fieldErrors: StateFlow<AuthValidator.RegisterErrors> = _fieldErrors.asStateFlow()

    private val _state = MutableStateFlow<Resource<User>?>(null)
    val state: StateFlow<Resource<User>?> = _state.asStateFlow()

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (_state.value is Resource.Loading) return

        // VR-01 do VR-05 — provera ide na klijentu pre bilo kakvog mreznog poziva.
        val errors = AuthValidator.validateRegister(name, email, password, confirmPassword)
        _fieldErrors.value = errors
        if (!errors.isValid) return

        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = authRepository.register(name.trim(), email.trim(), password, confirmPassword)
        }
    }
}
