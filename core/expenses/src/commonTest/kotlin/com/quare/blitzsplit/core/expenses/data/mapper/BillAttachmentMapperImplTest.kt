package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.PendingJpeg
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillAttachmentMapperImplTest {
    private val mapper = BillAttachmentMapperImpl()

    @Test
    fun `GIVEN a stored photo waiting to go up WHEN mapping to the domain THEN reads every field with its image`() {
        // Given
        val entity = entity(
            addedByUserId = null,
            pendingJpeg = jpeg,
            pendingSync = true,
        )

        // When
        val attachment = mapper.toDomain(entity)

        // Then
        assertEquals(
            attachment(
                addedByUserId = null,
                isPending = true,
                pendingImage = PendingJpeg(jpeg),
            ),
            attachment,
        )
    }

    @Test
    fun `GIVEN a synced photo WHEN mapping to the domain THEN reads it not pending and with no image`() {
        // Given
        val entity = entity()

        // When
        val attachment = mapper.toDomain(entity)

        // Then
        assertEquals(attachment(), attachment)
    }

    @Test
    fun `GIVEN a stored receipt WHEN mapping to the domain THEN reads it as the receipt`() {
        // Given
        val entity = entity(kind = "receipt")

        // When
        val attachment = mapper.toDomain(entity)

        // Then
        assertEquals(BillAttachmentKind.RECEIPT, attachment?.kind)
    }

    @Test
    fun `GIVEN a kind this version does not know WHEN mapping to the domain THEN leaves it out`() {
        // Given
        val entity = entity(kind = "video")

        // When
        val attachment = mapper.toDomain(entity)

        // Then
        assertNull(attachment)
    }

    @Test
    fun `GIVEN a new photo WHEN mapping it to store THEN copies every field with its JPEG and stays live`() {
        // Given
        val attachment = attachment(
            addedByUserId = null,
            isPending = true,
            pendingImage = PendingJpeg(jpeg),
        )

        // When
        val entity = mapper.toEntity(attachment)

        // Then
        assertEquals(
            entity(
                addedByUserId = null,
                pendingSync = true,
            ),
            entity.copy(pendingJpeg = null),
        )
        assertTrue(jpeg.contentEquals(entity.pendingJpeg))
    }

    @Test
    fun `GIVEN a receipt with no image WHEN mapping it to store THEN stores its kind code and no JPEG`() {
        // Given
        val attachment = attachment(kind = BillAttachmentKind.RECEIPT)

        // When
        val entity = mapper.toEntity(attachment)

        // Then
        assertEquals(entity(kind = "receipt"), entity)
    }

    @Test
    fun `GIVEN a server row WHEN mapping it to store THEN copies every field synced and with no JPEG`() {
        // Given
        val dto = dto()

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(entity(), entity)
    }

    @Test
    fun `GIVEN a server tombstone of a kind not known WHEN mapping it to store THEN keeps it deleted and its kind`() {
        // Given
        val dto = dto(
            kind = "video",
            isDeleted = true,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            entity(
                kind = "video",
                isDeleted = true,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a photo added before it had an account WHEN mapping it to send THEN sends the caller as who added it`() {
        // Given
        val entity = entity(
            addedByUserId = null,
            pendingJpeg = jpeg,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals(dto(addedByUserId = "user-caller"), dto)
    }

    @Test
    fun `GIVEN a removed photo someone else added WHEN mapping it to send THEN sends the caller as adder`() {
        // Given
        val entity = entity(
            addedByUserId = "user-ana",
            isDeleted = true,
            pendingSync = true,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals(
            dto(
                addedByUserId = "user-caller",
                isDeleted = true,
            ),
            dto,
        )
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    private fun attachment(
        kind: BillAttachmentKind = BillAttachmentKind.PHOTO,
        addedByUserId: String? = "user-ana",
        isPending: Boolean = false,
        pendingImage: PendingJpeg? = null,
    ): BillAttachment = BillAttachment(
        id = "photo-1",
        expenseId = "bill-1",
        groupId = "group-1",
        kind = kind,
        width = 1_600,
        height = 1_200,
        addedByUserId = addedByUserId,
        addedByMemberId = "member-ana",
        addedAt = 10L,
        updatedAt = 20L,
        isPending = isPending,
        pendingImage = pendingImage,
    )

    private fun entity(
        kind: String = "photo",
        addedByUserId: String? = "user-ana",
        isDeleted: Boolean = false,
        pendingJpeg: ByteArray? = null,
        pendingSync: Boolean = false,
    ): BillAttachmentEntity = BillAttachmentEntity(
        id = "photo-1",
        expenseId = "bill-1",
        groupId = "group-1",
        kind = kind,
        width = 1_600,
        height = 1_200,
        addedByUserId = addedByUserId,
        addedByMemberId = "member-ana",
        addedAt = 10L,
        updatedAt = 20L,
        isDeleted = isDeleted,
        pendingJpeg = pendingJpeg,
        pendingSync = pendingSync,
    )

    private fun dto(
        kind: String = "photo",
        addedByUserId: String? = "user-ana",
        isDeleted: Boolean = false,
    ): BillAttachmentDto = BillAttachmentDto(
        id = "photo-1",
        expenseId = "bill-1",
        groupId = "group-1",
        kind = kind,
        addedByUserId = addedByUserId,
        addedByMemberId = "member-ana",
        width = 1_600,
        height = 1_200,
        isDeleted = isDeleted,
        addedAt = 10L,
        updatedAt = 20L,
    )
}
