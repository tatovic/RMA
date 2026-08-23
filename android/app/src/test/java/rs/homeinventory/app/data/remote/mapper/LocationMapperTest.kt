package rs.homeinventory.app.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.remote.dto.LocationDto

class LocationMapperTest {

    private val dto = LocationDto(
        id = "loc-1",
        userId = "user-1",
        name = "Dnevna soba",
        description = null,
        itemCount = 3,
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-08-22T14:07:31.123Z"
    )

    @Test
    fun `toEntity mapira sva polja i podrazumeva SyncStatus SYNCED`() {
        val entity = dto.toEntity()

        assertEquals(dto.id, entity.id)
        assertEquals(dto.userId, entity.userId)
        assertEquals(dto.name, entity.name)
        assertEquals(dto.description, entity.description)
        assertEquals(DateMapper.isoToEpochMillis(dto.createdAt), entity.createdAt)
        assertEquals(DateMapper.isoToEpochMillis(dto.updatedAt), entity.updatedAt)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun `toDomain mapira Entity u Location`() {
        val domain = dto.toEntity().toDomain()

        assertEquals(dto.id, domain.id)
        assertEquals(dto.userId, domain.userId)
        assertEquals(dto.name, domain.name)
        assertEquals(dto.description, domain.description)
    }

    @Test
    fun `toRequestDto salje id samo pri kreiranju`() {
        val entity = dto.toEntity()

        val createRequest = entity.toRequestDto(includeId = true)
        val updateRequest = entity.toRequestDto(includeId = false)

        assertEquals(entity.id, createRequest.id)
        assertNull(updateRequest.id)
        assertEquals(entity.name, createRequest.name)
        assertEquals(entity.name, updateRequest.name)
    }
}
