package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.PendingJpeg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class BillAttachmentTest {
    @Test
    fun `GIVEN a photo of a bill WHEN reading where its image lives THEN is under its group and bill by its id`() {
        // Given
        val attachment = BillAttachment(
            id = "photo-1",
            expenseId = "bill-1",
            groupId = "group-1",
            kind = BillAttachmentKind.PHOTO,
            width = 800,
            height = 600,
            addedByUserId = null,
            addedByMemberId = null,
            addedAt = 1L,
            updatedAt = 1L,
            isPending = true,
            pendingImage = null,
        )

        // When
        val path = attachment.storagePath

        // Then
        assertEquals("group-1/bill-1/photo-1.jpg", path)
    }

    @Test
    fun `GIVEN the photo code WHEN reading the kind THEN is a photo with room for 5`() {
        // Given
        val code = "photo"

        // When
        val kind = BillAttachmentKind.fromCodeOrNull(code)

        // Then
        assertEquals(BillAttachmentKind.PHOTO, kind)
        assertEquals(5, kind?.limit)
    }

    @Test
    fun `GIVEN the receipt code WHEN reading the kind THEN is the single receipt`() {
        // Given
        val code = "receipt"

        // When
        val kind = BillAttachmentKind.fromCodeOrNull(code)

        // Then
        assertEquals(BillAttachmentKind.RECEIPT, kind)
        assertEquals(1, kind?.limit)
    }

    @Test
    fun `GIVEN a kind a newer version added WHEN reading it THEN is no kind this version knows`() {
        // Given
        val code = "video"

        // When
        val kind = BillAttachmentKind.fromCodeOrNull(code)

        // Then
        assertNull(kind)
    }

    @Test
    fun `GIVEN two images with the same bytes WHEN comparing them THEN they are equal with the same hash`() {
        // Given
        val first = PendingJpeg(byteArrayOf(1, 2, 3))
        val second = PendingJpeg(byteArrayOf(1, 2, 3))

        // When
        val equal = first == second

        // Then
        assertEquals(true, equal)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `GIVEN two images with different bytes WHEN comparing them THEN they differ`() {
        // Given
        val first = PendingJpeg(byteArrayOf(1, 2, 3))
        val second = PendingJpeg(byteArrayOf(1, 2, 4))

        // When
        val equal = first == second

        // Then
        assertFalse(equal)
    }

    @Test
    fun `GIVEN an image WHEN comparing it with its bare bytes THEN they differ`() {
        // Given
        val bytes = byteArrayOf(1, 2, 3)
        val image = PendingJpeg(bytes)

        // When
        val equal = image.equals(bytes)

        // Then
        assertFalse(equal)
    }

    @Test
    fun `GIVEN an image not up yet WHEN drawing it THEN is a JPEG data URI of its bytes in base64`() {
        // Given
        val image = PendingJpeg(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))

        // When
        val dataUri = image.dataUri

        // Then
        assertEquals("data:image/jpeg;base64,/9j/4A==", dataUri)
    }
}
