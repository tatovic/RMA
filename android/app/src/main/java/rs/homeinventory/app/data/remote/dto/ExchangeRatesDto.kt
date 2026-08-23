package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// tech.md sekcija 7.2 — polja API-ja su snake_case, DTO polja camelCase.
data class ExchangeRatesDto(
    @SerializedName("result") val result: String,
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("time_last_update_unix") val timeLastUpdateUnix: Long,
    @SerializedName("time_next_update_unix") val timeNextUpdateUnix: Long,
    @SerializedName("rates") val rates: Map<String, Double>
)
