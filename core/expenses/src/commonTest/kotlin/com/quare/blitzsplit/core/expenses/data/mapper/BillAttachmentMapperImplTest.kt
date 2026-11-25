package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillAttachmentDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.model.PhotoFrame
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillAttachmentMapperImplTest {
    private val mapper = BillAttachmentMapperImpl()

    @Test
    fun `GIVEN a stored photo waiting to go up WHEN mapping to the domain THEN reads every field with its image`() {
        // Given
        val row = row(
            addedByUserId = null,
            pendingSync = true,
            hasPendingJpeg = true,
        )

        // When
        val attachment = mapper.toDomain(row)

        // Then
        assertEquals(
            attachment(
                addedByUserId = null,
                isPending = true,
                hasPendingImage = true,
            ),
            attachment,
        )
    }

    @Test
    fun `GIVEN a synced photo WHEN mapping to the domain THEN reads it not pending and with no image`() {
        // Given
        val row = row()

        // When
        val attachment = mapper.toDomain(row)

        // Then
        assertEquals(attachment(), attachment)
    }

    @Test
    fun `GIVEN a stored receipt WHEN mapping to the domain THEN reads it as the receipt`() {
        // Given
        val row = row(kind = "receipt")

        // When
        val attachment = mapper.toDomain(row)

        // Then
        assertEquals(BillAttachmentKind.RECEIPT, attachment?.kind)
    }

    @Test
    fun `GIVEN a kind this version does not know WHEN mapping to the domain THEN leaves it out`() {
        // Given
        val row = row(kind = "video")

        // When
        val attachment = mapper.toDomain(row)

        // Then
        assertNull(attachment)
    }

    @Test
    fun `GIVEN a new photo WHEN mapping it to store THEN copies every field with its JPEG and stays live`() {
        // Given
        val attachment = attachment(
            addedByUserId = null,
            isPending = true,
            hasPendingImage = true,
        )

        // When
        val entity = mapper.toEntity(attachment, jpeg)

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
    fun `GIVEN a receipt WHEN mapping it to store THEN stores its kind code`() {
        // Given
        val attachment = attachment(kind = BillAttachmentKind.RECEIPT)

        // When
        val entity = mapper.toEntity(attachment, jpeg)

        // Then
        assertEquals(entity(kind = "receipt"), entity.copy(pendingJpeg = null))
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

    @Test
    fun `GIVEN a stored photo keeping its whole image WHEN mapping to the domain THEN reads its frame`() {
        // Given
        val row = row(
            frameLeft = FRAME_LEFT,
            frameTop = FRAME_TOP,
            frameWidth = FRAME_WIDTH,
            frameHeight = FRAME_HEIGHT,
        )

        // When
        val attachment = mapper.toDomain(row)

        // Then
        assertEquals(attachment(frame = zoomedOutFrame), attachment)
    }

    @Test
    fun `GIVEN a stored photo missing one side of its frame WHEN mapping to the domain THEN reads no frame`() {
        // Given
        val rows = listOf(
            row(
                frameTop = FRAME_TOP,
                frameWidth = FRAME_WIDTH,
                frameHeight = FRAME_HEIGHT,
            ),
            row(
                frameLeft = FRAME_LEFT,
                frameWidth = FRAME_WIDTH,
                frameHeight = FRAME_HEIGHT,
            ),
            row(
                frameLeft = FRAME_LEFT,
                frameTop = FRAME_TOP,
                frameHeight = FRAME_HEIGHT,
            ),
            row(
                frameLeft = FRAME_LEFT,
                frameTop = FRAME_TOP,
                frameWidth = FRAME_WIDTH,
            ),
        )

        // When
        val frames = rows.map { row -> mapper.toDomain(row)?.frame }

        // Then
        assertEquals(listOf(null, null, null, null), frames)
    }

    @Test
    fun `GIVEN a new photo keeping its whole image WHEN mapping it to store THEN stores its four frame sides`() {
        // Given
        val attachment = attachment(
            isPending = true,
            hasPendingImage = true,
            frame = zoomedOutFrame,
        )

        // When
        val entity = mapper.toEntity(attachment, jpeg)

        // Then
        assertEquals(
            entity(
                pendingSync = true,
                frameLeft = FRAME_LEFT,
                frameTop = FRAME_TOP,
                frameWidth = FRAME_WIDTH,
                frameHeight = FRAME_HEIGHT,
            ),
            entity.copy(pendingJpeg = null),
        )
    }

    @Test
    fun `GIVEN a server row of a photo with its frame WHEN mapping it to store THEN keeps the four sides`() {
        // Given
        val dto = dto(
            frameLeft = FRAME_LEFT,
            frameTop = FRAME_TOP,
            frameWidth = FRAME_WIDTH,
            frameHeight = FRAME_HEIGHT,
        )

        // When
        val entity = mapper.toEntity(dto)

        // Then
        assertEquals(
            entity(
                frameLeft = FRAME_LEFT,
                frameTop = FRAME_TOP,
                frameWidth = FRAME_WIDTH,
                frameHeight = FRAME_HEIGHT,
            ),
            entity,
        )
    }

    @Test
    fun `GIVEN a stored photo with its frame WHEN mapping it to send THEN sends the four sides`() {
        // Given
        val entity = entity(
            pendingJpeg = jpeg,
            pendingSync = true,
            frameLeft = FRAME_LEFT,
            frameTop = FRAME_TOP,
            frameWidth = FRAME_WIDTH,
            frameHeight = FRAME_HEIGHT,
        )

        // When
        val dto = mapper.toDto(entity, userId = "user-caller")

        // Then
        assertEquals(
            dto(
                addedByUserId = "user-caller",
                frameLeft = FRAME_LEFT,
                frameTop = FRAME_TOP,
                frameWidth = FRAME_WIDTH,
                frameHeight = FRAME_HEIGHT,
            ),
            dto,
        )
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1)

    // Why: zoomed out to fit, so the square goes past the image's left and right edges.
    private val zoomedOutFrame = PhotoFrame(
        left = FRAME_LEFT,
        top = FRAME_TOP,
        width = FRAME_WIDTH,
        height = FRAME_HEIGHT,
    )

    private fun attachment(
        kind: BillAttachmentKind = BillAttachmentKind.PHOTO,
        addedByUserId: String? = "user-ana",
        isPending: Boolean = false,
        hasPendingImage: Boolean = false,
        frame: PhotoFrame? = null,
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
        hasPendingImage = hasPendingImage,
        frame = frame,
    )

    private fun row(
        kind: String = "photo",
        addedByUserId: String? = "user-ana",
        pendingSync: Boolean = false,
        hasPendingJpeg: Boolean = false,
        frameLeft: Float? = null,
        frameTop: Float? = null,
        frameWidth: Float? = null,
        frameHeight: Float? = null,
    ): BillAttachmentRow = BillAttachmentRow(
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
        pendingSync = pendingSync,
        hasPendingJpeg = hasPendingJpeg,
        frameLeft = frameLeft,
        frameTop = frameTop,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
    )

    private fun entity(
        kind: String = "photo",
        addedByUserId: String? = "user-ana",
        isDeleted: Boolean = false,
        pendingJpeg: ByteArray? = null,
        pendingSync: Boolean = false,
        frameLeft: Float? = null,
        frameTop: Float? = null,
        frameWidth: Float? = null,
        frameHeight: Float? = null,
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
        frameLeft = frameLeft,
        frameTop = frameTop,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
    )

    private fun dto(
        kind: String = "photo",
        addedByUserId: String? = "user-ana",
        isDeleted: Boolean = false,
        frameLeft: Float? = null,
        frameTop: Float? = null,
        frameWidth: Float? = null,
        frameHeight: Float? = null,
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
        frameLeft = frameLeft,
        frameTop = frameTop,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
    )

    private companion object {
        const val FRAME_LEFT = -0.125f
        const val FRAME_TOP = 0.25f
        const val FRAME_WIDTH = 1.25f
        const val FRAME_HEIGHT = 0.5f
    }
}
