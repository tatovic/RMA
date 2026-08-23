package rs.homeinventory.app.domain.model

import java.time.LocalDate

// db.md sekcija 7 — domensko polje je "category.id"/"location.id"; ime se popunjava iz lokalnog kesa (join),
// moze biti null ako kategorija/lokacija jos nije ucitana.
data class ItemCategoryRef(val id: String, val name: String?)
data class ItemLocationRef(val id: String, val name: String?)

data class InventoryItem(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val category: ItemCategoryRef,
    val location: ItemLocationRef,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val quantity: Int,
    val purchasePrice: Long?,
    val estimatedValue: Long?,
    val currency: String,
    val purchaseDate: LocalDate?,
    val warrantyExpirationDate: LocalDate?,
    val seller: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val imagePath: String?
)
