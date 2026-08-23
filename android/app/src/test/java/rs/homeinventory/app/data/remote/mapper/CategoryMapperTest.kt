package rs.homeinventory.app.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import rs.homeinventory.app.data.remote.dto.CategoryDto

class CategoryMapperTest {

    private val dto = CategoryDto(
        id = "cat-1",
        name = "Elektronika",
        description = "Uredjaji i gadgeti",
        iconKey = "electronics",
        sortOrder = 2,
        itemCount = 5,
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-08-22T14:07:31.123Z"
    )

    @Test
    fun `toEntity mapira sva polja, itemCount se ne prenosi (server-only)`() {
        val entity = dto.toEntity()

        assertEquals(dto.id, entity.id)
        assertEquals(dto.name, entity.name)
        assertEquals(dto.description, entity.description)
        assertEquals(dto.iconKey, entity.iconKey)
        assertEquals(dto.sortOrder, entity.sortOrder)
        assertEquals(DateMapper.isoToEpochMillis(dto.updatedAt), entity.updatedAt)
    }

    @Test
    fun `toDomain mapira Entity u Category bez server-only polja`() {
        val domain = dto.toEntity().toDomain()

        assertEquals(dto.id, domain.id)
        assertEquals(dto.name, domain.name)
        assertEquals(dto.description, domain.description)
        assertEquals(dto.iconKey, domain.iconKey)
        assertEquals(dto.sortOrder, domain.sortOrder)
    }

    @Test
    fun `toRequestDto mapira domenski model u telo zahteva`() {
        val domain = dto.toEntity().toDomain()

        val request = domain.toRequestDto()

        assertEquals(domain.name, request.name)
        assertEquals(domain.description, request.description)
        assertEquals(domain.iconKey, request.iconKey)
        assertEquals(domain.sortOrder, request.sortOrder)
    }
}
