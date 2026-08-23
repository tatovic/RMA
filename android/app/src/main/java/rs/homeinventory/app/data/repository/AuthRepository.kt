package rs.homeinventory.app.data.repository

import retrofit2.Response
import rs.homeinventory.app.data.local.dao.UserDao
import rs.homeinventory.app.data.local.prefs.UserPreferences
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.AuthResponseDto
import rs.homeinventory.app.data.remote.dto.LoginRequestDto
import rs.homeinventory.app.data.remote.dto.RegisterRequestDto
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
    private val errorMessageProvider: ErrorMessageProvider
) {
    // FR-018 — registracija odmah prijavljuje korisnika, isti tok kao login.
    suspend fun register(name: String, email: String, password: String, confirmPassword: String): Resource<User> =
        authenticate { api.register(RegisterRequestDto(name, email, password, confirmPassword)) }

    suspend fun login(email: String, password: String): Resource<User> =
        authenticate { api.login(LoginRequestDto(email, password)) }

    // FR-011.
    suspend fun hasValidSession(): Boolean = prefs.hasValidSession()

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
