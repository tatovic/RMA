package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// backend/src/utils/serializer.js#serializeAdminUser (tiket 24).
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

// backend/src/modules/admin/admin.service.js#getStats.
data class AdminStatsDto(
    @SerializedName("registeredUsers") val registeredUsers: Int,
    @SerializedName("activeUsers") val activeUsers: Int,
    @SerializedName("deactivatedUsers") val deactivatedUsers: Int,
    @SerializedName("totalItems") val totalItems: Int,
    @SerializedName("totalCategories") val totalCategories: Int
)
