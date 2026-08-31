package rs.homeinventory.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rs.homeinventory.app.data.repository.AuthRepository
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SUPPORTED_CURRENCIES
import javax.inject.Inject

// SCR-09.
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val user: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    // US-15/BR-012 — lista valuta ponudjenih u padajucem meniju je zatvorena, uvek istih sest.
    val supportedCurrencies: List<String> = SUPPORTED_CURRENCIES

    private val _currencyUpdateError = MutableStateFlow<String?>(null)
    val currencyUpdateError: StateFlow<String?> = _currencyUpdateError.asStateFlow()

    // BR-005 — brise token i kompletan sadrzaj lokalne baze.
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loggedOut.value = true
        }
    }

    // Promena valute prikaza odmah preracunava sve zbirove u aplikaciji jer svi ekrani citaju
    // valutu iz istog User.currency polja u Room-u (currentUser flow).
    fun updateCurrency(currency: String) {
        if (currency == user.value?.currency) return
        viewModelScope.launch {
            when (val result = authRepository.updateCurrency(currency)) {
                is Resource.Error -> _currencyUpdateError.value = result.message
                else -> Unit
            }
        }
    }
}
