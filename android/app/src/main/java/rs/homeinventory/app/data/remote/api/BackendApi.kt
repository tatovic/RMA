package rs.homeinventory.app.data.remote.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import rs.homeinventory.app.data.remote.dto.AdminUserDto
import rs.homeinventory.app.data.remote.dto.AuthResponseDto
import rs.homeinventory.app.data.remote.dto.CategoriesResponseDto
import rs.homeinventory.app.data.remote.dto.CategoryDto
import rs.homeinventory.app.data.remote.dto.CategoryRequestDto
import rs.homeinventory.app.data.remote.dto.ChangePasswordRequestDto
import rs.homeinventory.app.data.remote.dto.ItemDto
import rs.homeinventory.app.data.remote.dto.ItemsResponseDto
import rs.homeinventory.app.data.remote.dto.LocationDto
import rs.homeinventory.app.data.remote.dto.LocationRequestDto
import rs.homeinventory.app.data.remote.dto.LocationsResponseDto
import rs.homeinventory.app.data.remote.dto.LoginRequestDto
import rs.homeinventory.app.data.remote.dto.RegisterRequestDto
import rs.homeinventory.app.data.remote.dto.UpdateUserRequestDto
import rs.homeinventory.app.data.remote.dto.UpdateUserStatusRequestDto
import rs.homeinventory.app.data.remote.dto.UserDto

// tech.md sekcija 6. Sve putanje su relativne u odnosu na BuildConfig.BACKEND_BASE_URL (koji vec sadrzi "/api/").
interface BackendApi {

    // ---- 6.2 Autentifikacija (bez tokena) ----
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<AuthResponseDto>

    // ---- 6.3 Korisnik ----
    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

    @PATCH("users/me")
    suspend fun updateMe(@Body body: UpdateUserRequestDto): Response<UserDto>

    @POST("users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): Response<Unit>

    // ---- 6.4 Predmeti ----
    // Query parametar je "since", ne "updatedSince" — backend/src/modules/items/items.schema.js
    // (tech.md sekcija 6.4 ovde odstupa od stvarne implementacije backenda).
    @GET("items")
    suspend fun getItems(@Query("since") since: String? = null): Response<ItemsResponseDto>

    @GET("items/{id}")
    suspend fun getItem(@Path("id") id: String): Response<ItemDto>

    @POST("items")
    suspend fun createItem(@Body body: ItemDto): Response<ItemDto>

    @PUT("items/{id}")
    suspend fun updateItem(@Path("id") id: String, @Body body: ItemDto): Response<ItemDto>

    @DELETE("items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<Unit>

    // ---- 6.5 Kategorije i lokacije ----
    @GET("categories")
    suspend fun getCategories(): Response<CategoriesResponseDto>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryRequestDto): Response<CategoryDto>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryRequestDto): Response<CategoryDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    @GET("locations")
    suspend fun getLocations(): Response<LocationsResponseDto>

    @POST("locations")
    suspend fun createLocation(@Body body: LocationRequestDto): Response<LocationDto>

    @PUT("locations/{id}")
    suspend fun updateLocation(@Path("id") id: String, @Body body: LocationRequestDto): Response<LocationDto>

    @DELETE("locations/{id}")
    suspend fun deleteLocation(@Path("id") id: String): Response<Unit>

    // ---- 6.6 Administracija (backend modul jos nije implementiran, ugovor po tech.md) ----
    @GET("admin/statistics")
    suspend fun getStatistics(): Response<JsonObject>

    @GET("admin/users")
    suspend fun getAdminUsers(): Response<List<AdminUserDto>>

    @PATCH("admin/users/{id}/status")
    suspend fun updateUserStatus(@Path("id") id: String, @Body body: UpdateUserStatusRequestDto): Response<Unit>
}
