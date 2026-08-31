package rs.homeinventory.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import rs.homeinventory.app.data.local.HomeInventoryDatabase
import rs.homeinventory.app.data.local.dao.UserDao
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.AuthResponseDto
import rs.homeinventory.app.data.remote.dto.LoginRequestDto
import rs.homeinventory.app.data.remote.dto.RegisterRequestDto
import rs.homeinventory.app.data.remote.dto.UpdateUserRequestDto
import rs.homeinventory.app.data.remote.mapper.toDomain
import rs.homeinventory.app.data.remote.mapper.toEntity
import rs.homeinventory.app.domain.model.User
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

// Jedina tacka pristupa auth podacima — DEP-03 (Fragment/ViewModel nikad ne diraju BackendApi/UserDao/UserPreferences direktno).
@Singleton
class AuthRepository @Inject constructor(
    private val api: BackendApi,
    private val userDao: UserDao,
    private val prefs: UserPreferences,
    private val database: HomeInventoryDatabase,
    private val errorMessageProvider: ErrorMessageProvider
) {
    // FR-018 — registracija odmah prijavljuje korisnika, isti tok kao login.
    suspend fun register(name: String, email: String, password: String, confirmPassword: String): Resource<User> =
        authenticate { api.register(RegisterRequestDto(name, email, password, confirmPassword)) }

    suspend fun login(email: String, password: String): Resource<User> =
        authenticate { api.login(LoginRequestDto(email, password)) }

    // FR-011.
    suspend fun hasValidSession(): Boolean = prefs.hasValidSession()

    // SCR-09 — Profil prikazuje ime/email/rolu prijavljenog korisnika.
    val currentUser: Flow<User?> = userDao.observeCurrentUser().map { it?.toDomain() }

    // US-03/BR-005 — brise token i kompletan sadrzaj lokalne baze.
    suspend fun logout() {
        prefs.clearSession()
        database.clearAllData()
    }

    // US-15/BR-012 — valuta prikaza se bira u Profilu iz zatvorene liste od sest podrzanih valuta.
    suspend fun updateCurrency(currency: String): Resource<User> =
        when (val result = safeApiCall(errorMessageProvider) { api.updateMe(UpdateUserRequestDto(currency = currency)) }) {
            is Resource.Success -> {
                val entity = result.data.toEntity()
                userDao.upsert(entity)
                Resource.Success(entity.toDomain())
            }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }

    private suspend fun authenticate(call: suspend () -> Response<AuthResponseDto>): Resource<User> =
        when (val result = safeApiCall(errorMessageProvider, call)) {
            is Resource.Success -> {
                val auth = result.data
                prefs.saveSession(auth.token, auth.user.id, auth.user.role)
                val entity = auth.user.toEntity()
                userDao.upsert(entity)
                Resource.Success(entity.toDomain())
            }
            is Resource.Error -> result
            Resource.Loading -> Resource.Loading
        }
}
