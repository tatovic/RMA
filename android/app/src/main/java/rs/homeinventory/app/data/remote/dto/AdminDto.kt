package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// tech.md sekcija 6.6. Backend modul admin/statistics jos nije implementiran (van obima tiketa 10) —
// ugovor je definisan po tech.md da bi BackendApi bio kompletan; GET /api/admin/statistics zato
// vraca sirov JsonObject umesto tipizovane DTO klase dok tacan oblik odgovora ne bude poznat.
data class AdminUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("itemCount") val itemCount: Int
)

data class UpdateUserStatusRequestDto(
    @SerializedName("isActive") val isActive: Boolean
)
