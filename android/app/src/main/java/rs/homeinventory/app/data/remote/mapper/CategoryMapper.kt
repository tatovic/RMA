package rs.homeinventory.app.data.remote.mapper

import rs.homeinventory.app.data.local.entity.CategoryEntity
import rs.homeinventory.app.data.remote.dto.CategoryDto
import rs.homeinventory.app.data.remote.dto.CategoryRequestDto
import rs.homeinventory.app.domain.model.Category

fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    description = description,
    iconKey = iconKey,
    sortOrder = sortOrder,
    updatedAt = DateMapper.isoToEpochMillis(updatedAt)
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    description = description,
    iconKey = iconKey,
    sortOrder = sortOrder
)

fun Category.toRequestDto(): CategoryRequestDto = CategoryRequestDto(
    name = name,
    description = description,
    iconKey = iconKey,
    sortOrder = sortOrder
)
