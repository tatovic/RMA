package rs.homeinventory.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import rs.homeinventory.app.data.local.entity.ExchangeRateEntity

// Kes eksternog Currency API-ja, TTL od 24h se proverava u Kotlin sloju preko fetchedAt.
@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rates WHERE baseCode = :baseCode AND targetCode = :targetCode")
    suspend fun getRate(baseCode: String, targetCode: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE baseCode = :baseCode")
    suspend fun getAllForBase(baseCode: String): List<ExchangeRateEntity>

    @Upsert suspend fun upsertAll(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates")
    suspend fun clear()
}
