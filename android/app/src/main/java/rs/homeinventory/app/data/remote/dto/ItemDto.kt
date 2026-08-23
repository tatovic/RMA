package rs.homeinventory.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// Oblik odgovora — db.md sekcija 7 (autoritativna tabela mapiranja), backend/src/utils/serializer.js#serializeItem.
// Ista klasa se koristi i za zahtev (create/update, gde su userId/createdAt/updatedAt/deletedAt nepoznati pa se izostavljaju)
// i za odgovor servera (gde su sva polja popunjena).
data class ItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("locationId") val locationId: String,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("serialNumber") val serialNumber: String?,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("purchasePrice") val purchasePrice: Long?,
    @SerializedName("estimatedValue") val estimatedValue: Long?,
    @SerializedName("currency") val currency: String,
    @SerializedName("purchaseDate") val purchaseDate: String?,
    @SerializedName("warrantyExpirationDate") val warrantyExpirationDate: String?,
    @SerializedName("seller") val seller: String?,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String? = null,
    // Obavezno pri PUT-u (DB-RULE-04, LWW provera), izostavljeno pri POST-u.
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("deletedAt") val deletedAt: String? = null
)

data class ItemsResponseDto(
    @SerializedName("items") val items: List<ItemDto>,
    @SerializedName("serverTime") val serverTime: String
)
