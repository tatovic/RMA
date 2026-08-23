package rs.homeinventory.app.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Oblik greske sa servera — tech.md sekcija 6.1.
data class ErrorResponseDto(
    @SerializedName("error") val error: ErrorBodyDto
)

data class ErrorBodyDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    // Prisutno samo kod VALIDATION_ERROR (lista polja) ili pri CATEGORY_IN_USE / LOCATION_IN_USE / SYNC_CONFLICT — oblik zavisi od koda.
    @SerializedName("details") val details: JsonElement? = null
)
