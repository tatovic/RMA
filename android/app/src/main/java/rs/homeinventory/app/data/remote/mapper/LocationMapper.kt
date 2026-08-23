package rs.homeinventory.app.data.remote.mapper

import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.local.entity.LocationEntity
import rs.homeinventory.app.data.remote.dto.LocationDto
import rs.homeinventory.app.data.remote.dto.LocationRequestDto
import rs.homeinventory.app.domain.model.Location

fun LocationDto.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): LocationEntity = LocationEntity(
    id = id,
    userId = userId,
    name = name,
    description = description,
    createdAt = DateMapper.isoToEpochMillis(createdAt),
    updatedAt = DateMapper.isoToEpochMillis(updatedAt),
    syncStatus = syncStatus
)

fun LocationEntity.toDomain(): Location = Location(
    id = id,
    userId = userId,
    name = name,
    description = description
)

// Zahtev pri push-u (kreiranje/izmena) — id se salje samo pri kreiranju.
fun LocationEntity.toRequestDto(includeId: Boolean): LocationRequestDto = LocationRequestDto(
    id = if (includeId) id else null,
    name = name,
    description = description
)
