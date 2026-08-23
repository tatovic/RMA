package rs.homeinventory.app.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import rs.homeinventory.app.data.remote.dto.ExchangeRatesDto

// tech.md sekcija 7.2 — open.er-api.com, bez autentifikacije. Nikada ne koristi backend OkHttp klijent (SEC).
interface CurrencyApi {
    @GET("latest/{base}")
    suspend fun getRates(@Path("base") base: String = "EUR"): Response<ExchangeRatesDto>
}
