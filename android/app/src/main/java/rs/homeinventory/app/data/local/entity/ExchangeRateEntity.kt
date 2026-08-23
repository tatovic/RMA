package rs.homeinventory.app.data.local.entity

import androidx.room.Entity

// Kes eksternog Currency API-ja. Ne postoji na nasem backendu.
@Entity(tableName = "exchange_rates", primaryKeys = ["baseCode", "targetCode"])
data class ExchangeRateEntity(
    val baseCode: String,     // uvek "EUR" - API se poziva sa EUR bazom
    val targetCode: String,   // RSD, USD, CHF, GBP, BAM, EUR
    val rate: Double,         // kurs, NIJE novac - Double je ovde ispravan
    val fetchedAt: Long       // epoch millis, za TTL od 24h
)
