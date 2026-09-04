package rs.homeinventory.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.homeinventory.app.data.local.dao.ExchangeRateDao
import rs.homeinventory.app.data.local.entity.ExchangeRateEntity
import rs.homeinventory.app.data.remote.api.CurrencyApi
import rs.homeinventory.app.util.ErrorCode
import rs.homeinventory.app.util.ErrorMessageProvider
import rs.homeinventory.app.util.Resource
import rs.homeinventory.app.util.SUPPORTED_CURRENCIES
import rs.homeinventory.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_CURRENCY = "EUR"
private const val CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000L

// tech.md sekcija 7 — jedina tacka pristupa kursnoj listi (DEP-03). Poziva CurrencyApi preko zasebnog
// OkHttp klijenta bez naseg JWT tokena (NetworkModule @Named("currency")). BR-012/BR-013/FR-063..069.
@Singleton
class CurrencyRepository @Inject constructor(
    private val api: CurrencyApi,
    private val dao: ExchangeRateDao,
    private val errorMessageProvider: ErrorMessageProvider
) {
    // FR-064 — kes stariji od 24h se pokusava osveziti; svez kes ne pokrece mrezni poziv.
    // FR-065 — neuspeh mreze pada na poslednji poznati kes umesto da rusi poziv.
    suspend fun getRates(): Resource<Map<String, Double>> = withContext(Dispatchers.IO) {
        val cached = dao.getAllForBase(BASE_CURRENCY)
        val cachedRates = cached.toRateMap()
        val isFresh = cached.isNotEmpty() &&
            cached.minOf { it.fetchedAt } > System.currentTimeMillis() - CACHE_TTL_MILLIS

        if (isFresh) return@withContext Resource.Success(cachedRates)

        when (val result = safeApiCall(errorMessageProvider) { api.getRates(BASE_CURRENCY) }) {
            is Resource.Success -> {
                val dto = result.data
                // tech.md 7.2 — "result" mora eksplicitno javiti uspeh, HTTP status se ne uzima zdravo za gotovo.
                if (dto.result != "success") return@withContext fallbackOrError(cachedRates)

                // BR-012 — od 166 valuta cuva se samo tri podrzane.
                val filtered = dto.rates.filterKeys { it in SUPPORTED_CURRENCIES }
                val now = System.currentTimeMillis()
                dao.upsertAll(filtered.map { (code, rate) -> ExchangeRateEntity(BASE_CURRENCY, code, rate, now) })
                Resource.Success(filtered)
            }
            is Resource.Error -> fallbackOrError(cachedRates)
            Resource.Loading -> Resource.Loading
        }
    }

    private fun fallbackOrError(cachedRates: Map<String, Double>): Resource<Map<String, Double>> =
        if (cachedRates.isNotEmpty()) {
            Resource.Success(cachedRates)
        } else {
            // BR-013 — kad kursa nema ni u mrezi ni u kesu, poziv se prijavljuje kao greska umesto da se izmisli kurs 1.0.
            Resource.Error(ErrorCode.CURRENCY_UNAVAILABLE, errorMessageProvider.message(ErrorCode.CURRENCY_UNAVAILABLE))
        }

    private fun List<ExchangeRateEntity>.toRateMap(): Map<String, Double> =
        associate { it.targetCode to it.rate }
}
