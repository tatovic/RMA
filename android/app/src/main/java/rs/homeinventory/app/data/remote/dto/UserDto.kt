package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// tech.md sekcija 6.3; backend/src/utils/serializer.js#serializeUser — bez updatedAt, samo createdAt.
data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("currency") val currency: String,
    @SerializedName("createdAt") val createdAt: String
)

data class UpdateUserRequestDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("currency") val currency: String? = null
)

data class ChangePasswordRequestDto(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String
)
