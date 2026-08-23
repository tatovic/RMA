package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// backend/src/utils/serializer.js#serializeCategory.
data class CategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("iconKey") val iconKey: String?,
    @SerializedName("sortOrder") val sortOrder: Int,
    @SerializedName("itemCount") val itemCount: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CategoriesResponseDto(
    @SerializedName("categories") val categories: List<CategoryDto>
)

// Zahtev za kreiranje/izmenu — ADMIN, backend/src/modules/categories/categories.schema.js.
data class CategoryRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("iconKey") val iconKey: String? = null,
    @SerializedName("sortOrder") val sortOrder: Int = 0
)
