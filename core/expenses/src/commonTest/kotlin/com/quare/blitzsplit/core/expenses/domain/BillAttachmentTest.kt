package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import kotlin.test.Test
import kotlin.test.assertEquals
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
            hasPendingImage = true,
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
}
