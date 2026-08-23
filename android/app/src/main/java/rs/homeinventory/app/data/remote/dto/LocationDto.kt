package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// backend/src/utils/serializer.js#serializeLocation.
data class LocationDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("itemCount") val itemCount: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class LocationsResponseDto(
    @SerializedName("locations") val locations: List<LocationDto>
)

// Zahtev za kreiranje/izmenu. `id` generise klijent i salje se samo pri kreiranju
// (backend/src/modules/locations/locations.schema.js — idempotentno po id-ju).
data class LocationRequestDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)
