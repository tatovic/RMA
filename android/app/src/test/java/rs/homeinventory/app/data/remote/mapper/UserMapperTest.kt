package rs.homeinventory.app.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import rs.homeinventory.app.data.local.UserRole
import rs.homeinventory.app.data.remote.dto.UserDto

class UserMapperTest {

    private val dto = UserDto(
        id = "user-1",
        name = "Marko",
        email = "marko@primer.rs",
        role = "ADMIN",
        isActive = true,
        currency = "RSD",
        createdAt = "2026-08-22T14:07:31.123Z"
    )

    @Test
    fun `toEntity mapira rolu iz stringa u UserRole`() {
        val entity = dto.toEntity()

        assertEquals(UserRole.ADMIN, entity.role)
    }

    @Test
    fun `toEntity popunjava updatedAt vrednoscu createdAt jer server ne vraca updatedAt`() {
        val entity = dto.toEntity()

        assertEquals(entity.createdAt, entity.updatedAt)
        assertEquals(DateMapper.isoToEpochMillis(dto.createdAt), entity.createdAt)
    }

    @Test
    fun `toEntity mapira ostala polja bez izmena`() {
        val entity = dto.toEntity()

        assertEquals(dto.id, entity.id)
        assertEquals(dto.name, entity.name)
        assertEquals(dto.email, entity.email)
        assertEquals(dto.isActive, entity.isActive)
        assertEquals(dto.currency, entity.currency)
    }

    @Test
    fun `toDomain mapira Entity u domenski model User`() {
        val domain = dto.toEntity().toDomain()

        assertEquals(dto.id, domain.id)
        assertEquals(dto.name, domain.name)
        assertEquals(dto.email, domain.email)
        assertEquals(UserRole.ADMIN, domain.role)
        assertEquals(dto.isActive, domain.isActive)
        assertEquals(dto.currency, domain.currency)
    }
}
