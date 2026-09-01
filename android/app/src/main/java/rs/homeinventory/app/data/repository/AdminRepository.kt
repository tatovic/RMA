package rs.homeinventory.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.homeinventory.app.data.local.dao.CategoryDao
import rs.homeinventory.app.data.remote.api.BackendApi
import rs.homeinventory.app.data.remote.dto.AdminStatsDto
import rs.homeinventory.app.data.remote.dto.AdminUserDto
import rs.homeinventory.app.data.remote.dto.CategoryDto
import rs.homeinventory.app.data.remote.dto.CategoryRequestDto
import rs.homeinventory.app.data.remote.dto.UpdateUserStatusRequestDto
import rs.homeinventory.app.data.remote.mapper.toEntity
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

// Jedina tacka pristupa administratorskim podacima — DEP-03. Za razliku od ItemRepository, ekrani
// se ne pune iz Room-a: brojevi su globalni (svi korisnici), ne per-user, pa nema smisla da zive u
// istom kesu koji koristi obican korisnicki UI (tiket 25, backend/src/modules/admin iz tiketa 24).
@Singleton
class AdminRepository @Inject constructor(
    private val api: BackendApi,
    private val categoryDao: CategoryDao,
    private val errorMessageProvider: ErrorMessageProvider
) {
    suspend fun getStats(): Resource<AdminStatsDto> = withContext(Dispatchers.IO) {
        safeApiCall(errorMessageProvider) { api.getAdminStats() }
    }

    // OWN-06 — server vec ne vraca nikakav sadrzaj tudjeg inventara, samo brojac (backend/src/utils/serializer.js).
    suspend fun getUsers(): Resource<List<AdminUserDto>> = withContext(Dispatchers.IO) {
        safeApiCall(errorMessageProvider) { api.getAdminUsers() }
    }

    // BR-004/BR-008 — potvrda i provera samodeaktivacije su na ekranu; server je konacna potvrda (409 CANNOT_DEACTIVATE_SELF).
    suspend fun updateUserStatus(id: String, isActive: Boolean): Resource<AdminUserDto> = withContext(Dispatchers.IO) {
        safeApiCall(errorMessageProvider) { api.updateUserStatus(id, UpdateUserStatusRequestDto(isActive)) }
    }

    // SCR-13 — spisak globalnih kategorija sa brojem predmeta svih korisnika (backend/src/utils/serializer.js#serializeCategory).
    suspend fun getCategories(): Resource<List<CategoryDto>> = withContext(Dispatchers.IO) {
        safeApiCall(errorMessageProvider) { api.getCategories() }.let { result ->
            when (result) {
                is Resource.Success -> Resource.Success(result.data.categories)
                is Resource.Error -> Resource.Error(result.code, result.message)
                Resource.Loading -> Resource.Loading
            }
        }
    }

    suspend fun createCategory(name: String): Resource<Unit> = withContext(Dispatchers.IO) {
        toUnitResource(safeApiCall(errorMessageProvider) { api.createCategory(CategoryRequestDto(name = name)) })
    }

    // Cuva postojeci opis/ikonicu/redosled — ekran menja samo naziv (VR-19-slican obrazac kao lokacije, tiket 17).
    suspend fun renameCategory(current: CategoryDto, newName: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val dto = CategoryRequestDto(
            name = newName,
            description = current.description,
            iconKey = current.iconKey,
            sortOrder = current.sortOrder
        )
        toUnitResource(safeApiCall(errorMessageProvider) { api.updateCategory(current.id, dto) })
    }

    // BR-014-slicno pravilo za kategorije (CATEGORY_IN_USE) — broj predmeta se proverava na ekranu pre poziva.
    suspend fun deleteCategory(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        when (val result = safeApiCall(errorMessageProvider) { api.deleteCategory(id) }) {
            is Resource.Success -> {
                categoryDao.delete(id)
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(result.code, result.message)
            Resource.Loading -> Resource.Loading
        }
    }

    private suspend fun toUnitResource(result: Resource<CategoryDto>): Resource<Unit> =
        when (result) {
            is Resource.Success -> {
                categoryDao.upsertAll(listOf(result.data.toEntity()))
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(result.code, result.message)
            Resource.Loading -> Resource.Loading
        }
}
