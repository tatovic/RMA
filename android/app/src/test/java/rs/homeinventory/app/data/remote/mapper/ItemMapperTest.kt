package rs.homeinventory.app.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import rs.homeinventory.app.data.local.SyncStatus
import rs.homeinventory.app.data.remote.dto.ItemDto
import java.time.LocalDate

class ItemMapperTest {

    // Primer iz tech.md sekcija 6.4.
    private val serverDto = ItemDto(
        id = "550e8400-e29b-41d4-a716-446655440000",
        userId = "user-1",
        name = "Samsung TV 55\"",
        description = null,
        categoryId = "cat-1",
        locationId = "loc-1",
        manufacturer = "Samsung",
        model = "UE55TU7092",
        serialNumber = "SN123456",
        quantity = 1,
        purchasePrice = 8990000L,
        estimatedValue = null,
        currency = "RSD",
        purchaseDate = "2024-03-15",
        warrantyExpirationDate = "2026-03-15",
        seller = "Tehnomanija",
        notes = null,
        createdAt = "2026-08-22T14:07:31.123Z",
        updatedAt = "2026-08-22T14:07:31.123Z",
        deletedAt = null
    )

    @Test
    fun `toEntity cuva postojecu lokalnu putanju do fotografije (DB-RULE-02)`() {
        val entity = serverDto.toEntity(keepImagePath = "local/photo.jpg")

        assertEquals("local/photo.jpg", entity.imagePath)
    }

    @Test
    fun `toEntity postavlja imagePath na null kada lokalna vrednost ne postoji`() {
        val entity = serverDto.toEntity(keepImagePath = null)

        assertNull(entity.imagePath)
    }

    @Test
    fun `toEntity mapira sva polja i podrazumeva SyncStatus SYNCED`() {
        val entity = serverDto.toEntity(keepImagePath = null)

        assertEquals(serverDto.id, entity.id)
        assertEquals(serverDto.userId, entity.userId)
        assertEquals(serverDto.name, entity.name)
        assertEquals(serverDto.categoryId, entity.categoryId)
        assertEquals(serverDto.locationId, entity.locationId)
        assertEquals(serverDto.manufacturer, entity.manufacturer)
        assertEquals(serverDto.quantity, entity.quantity)
        assertEquals(serverDto.purchasePrice, entity.purchasePrice)
        assertEquals(serverDto.currency, entity.currency)
        assertEquals(serverDto.purchaseDate, entity.purchaseDate)
        assertEquals(serverDto.warrantyExpirationDate, entity.warrantyExpirationDate)
        assertEquals(DateMapper.isoToEpochMillis(serverDto.createdAt!!), entity.createdAt)
        assertEquals(DateMapper.isoToEpochMillis(serverDto.updatedAt!!), entity.updatedAt)
        assertNull(entity.deletedAt)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }

    @Test
    fun `toEntity prihvata prosledjen SyncStatus`() {
        val entity = serverDto.toEntity(keepImagePath = null, syncStatus = SyncStatus.PENDING_UPDATE)

        assertEquals(SyncStatus.PENDING_UPDATE, entity.syncStatus)
    }

    @Test
    fun `toEntity prevodi deletedAt kada je prisutan (tombstone)`() {
        val tombstone = serverDto.copy(deletedAt = "2026-08-23T00:00:00.000Z")

        val entity = tombstone.toEntity(keepImagePath = null)

        assertEquals(DateMapper.isoToEpochMillis("2026-08-23T00:00:00.000Z"), entity.deletedAt)
    }

    @Test
    fun `toDto mapira Entity nazad u ItemDto radi push-a`() {
        val entity = serverDto.toEntity(keepImagePath = "local/photo.jpg")

        val dto = entity.toDto()

        assertEquals(serverDto.id, dto.id)
        assertEquals(serverDto.userId, dto.userId)
        assertEquals(serverDto.name, dto.name)
        assertEquals(serverDto.categoryId, dto.categoryId)
        assertEquals(serverDto.locationId, dto.locationId)
        assertEquals(serverDto.purchasePrice, dto.purchasePrice)
        assertEquals(serverDto.purchaseDate, dto.purchaseDate)
        assertEquals(serverDto.createdAt, dto.createdAt)
        assertEquals(serverDto.updatedAt, dto.updatedAt)
        assertNull(dto.deletedAt)
    }

    @Test
    fun `toDomain parsira kalendarske datume u LocalDate i prosledjuje nazive kategorije i lokacije`() {
        val entity = serverDto.toEntity(keepImagePath = "local/photo.jpg")

        val domain = entity.toDomain(categoryName = "Elektronika", locationName = "Dnevna soba")

        assertEquals(LocalDate.of(2024, 3, 15), domain.purchaseDate)
        assertEquals(LocalDate.of(2026, 3, 15), domain.warrantyExpirationDate)
        assertEquals("cat-1", domain.category.id)
        assertEquals("Elektronika", domain.category.name)
        assertEquals("loc-1", domain.location.id)
        assertEquals("Dnevna soba", domain.location.name)
        assertEquals("local/photo.jpg", domain.imagePath)
    }

    @Test
    fun `toDomain dozvoljava null nazive kada kes jos nije ucitan`() {
        val entity = serverDto.toEntity(keepImagePath = null)

        val domain = entity.toDomain()

        assertNull(domain.category.name)
        assertNull(domain.location.name)
    }

    @Test
    fun `toDomain vraca null za kalendarski datum kada ga nema`() {
        val withoutDates = serverDto.copy(purchaseDate = null, warrantyExpirationDate = null)
        val entity = withoutDates.toEntity(keepImagePath = null)

        val domain = entity.toDomain()

        assertNull(domain.purchaseDate)
        assertNull(domain.warrantyExpirationDate)
    }
}
