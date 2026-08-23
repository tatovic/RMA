package rs.homeinventory.app.data.remote.mapper

import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.entity.InventoryItemEntity
import rs.homeinventory.app.data.remote.dto.ItemDto
import rs.homeinventory.app.domain.model.InventoryItem
import rs.homeinventory.app.domain.model.ItemCategoryRef
import rs.homeinventory.app.domain.model.ItemLocationRef

// DB-RULE-02 (FR-085) — imagePath postoji samo lokalno i mora se sacuvati pri svakom pull-u sa servera;
// prosledjuje se eksplicitno kao parametar, nikad se ne cita sa servera.
fun ItemDto.toEntity(keepImagePath: String?, syncStatus: SyncStatus = SyncStatus.SYNCED): InventoryItemEntity =
    InventoryItemEntity(
        id = id,
        userId = requireNotNull(userId) { "userId je obavezan u odgovoru servera" },
        name = name,
        description = description,
        categoryId = categoryId,
        locationId = locationId,
        manufacturer = manufacturer,
        model = model,
        serialNumber = serialNumber,
        quantity = quantity,
        purchasePrice = purchasePrice,
        estimatedValue = estimatedValue,
        currency = currency,
        purchaseDate = purchaseDate,
        warrantyExpirationDate = warrantyExpirationDate,
        seller = seller,
        notes = notes,
        createdAt = DateMapper.isoToEpochMillis(requireNotNull(createdAt) { "createdAt je obavezan u odgovoru servera" }),
        updatedAt = DateMapper.isoToEpochMillis(requireNotNull(updatedAt) { "updatedAt je obavezan u odgovoru servera" }),
        deletedAt = DateMapper.isoToEpochMillisOrNull(deletedAt),
        imagePath = keepImagePath,
        syncStatus = syncStatus
    )

// Za push (create/update) — server ignorise userId iz tela (OWN-02), ali ga saljemo jer ne smeta.
fun InventoryItemEntity.toDto(): ItemDto = ItemDto(
    id = id,
    userId = userId,
    name = name,
    description = description,
    categoryId = categoryId,
    locationId = locationId,
    manufacturer = manufacturer,
    model = model,
    serialNumber = serialNumber,
    quantity = quantity,
    purchasePrice = purchasePrice,
    estimatedValue = estimatedValue,
    currency = currency,
    purchaseDate = purchaseDate,
    warrantyExpirationDate = warrantyExpirationDate,
    seller = seller,
    notes = notes,
    createdAt = DateMapper.epochMillisToIso(createdAt),
    updatedAt = DateMapper.epochMillisToIso(updatedAt),
    deletedAt = DateMapper.epochMillisToIsoOrNull(deletedAt)
)

// categoryName/locationName dolaze iz lokalnog kesa (join u repository sloju); ovde nisu dostupni bez njega.
fun InventoryItemEntity.toDomain(categoryName: String? = null, locationName: String? = null): InventoryItem =
    InventoryItem(
        id = id,
        userId = userId,
        name = name,
        description = description,
        category = ItemCategoryRef(categoryId, categoryName),
        location = ItemLocationRef(locationId, locationName),
        manufacturer = manufacturer,
        model = model,
        serialNumber = serialNumber,
        quantity = quantity,
        purchasePrice = purchasePrice,
        estimatedValue = estimatedValue,
        currency = currency,
        purchaseDate = DateMapper.parseLocalDate(purchaseDate),
        warrantyExpirationDate = DateMapper.parseLocalDate(warrantyExpirationDate),
        seller = seller,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        imagePath = imagePath
    )
