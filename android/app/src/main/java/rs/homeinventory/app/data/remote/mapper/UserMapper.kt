package rs.homeinventory.app.data.remote.mapper

import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.data.local.entity.UserEntity
import rs.homeinventory.app.data.remote.dto.UserDto
import rs.homeinventory.app.domain.model.User

// backend/src/utils/serializer.js#serializeUser ne vraca updatedAt — UserEntity.updatedAt se
// pri mapiranju popunjava vrednoscu createdAt jer server tu vrednost ne salje.
fun UserDto.toEntity(): UserEntity {
    val createdMillis = DateMapper.isoToEpochMillis(createdAt)
    return UserEntity(
        id = id,
        name = name,
        email = email,
        // ERR-02 — nepoznata rola sa servera ne sme da rusi mapiranje; pada na najmanje privilegovanu
        // (tiket 28, nalaz C10). `requireAdmin` na serveru je ionako prava odbrana, ovo je samo prikaz.
        role = UserRole.entries.find { it.name == role } ?: UserRole.USER,
        isActive = isActive,
        currency = currency,
        createdAt = createdMillis,
        updatedAt = createdMillis
    )
}

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    role = role,
    isActive = isActive,
    currency = currency
)
