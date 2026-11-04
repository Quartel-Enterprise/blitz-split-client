package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLink
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BillCommentLinkTest {
    @Test
    fun `GIVEN a bill and a comment WHEN building the comment link THEN points at both on blitzsplit dot com`() {
        // Given
        val expenseId = BILL_ID

        // When
        val link = BillCommentLink.of(expenseId, COMMENT_ID)

        // Then
        assertEquals("https://blitzsplit.com/b/$BILL_ID?comentario=$COMMENT_ID", link)
    }

    @Test
    fun `GIVEN the link the app builds WHEN reading it THEN gets the bill and the comment back`() {
        // Given
        val url = BillCommentLink.of(BILL_ID, COMMENT_ID)

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, COMMENT_ID), target)
    }

    @Test
    fun `GIVEN a bill id in upper case WHEN reading the link THEN gets the bill id in lower case`() {
        // Given
        val url = "https://blitzsplit.com/b/${BILL_ID.uppercase()}?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BILL_ID, target?.expenseId)
    }

    @Test
    fun `GIVEN a link on another host over http WHEN reading it THEN gets the bill and the comment`() {
        // Given
        val url = "http://localhost:5173/b/$BILL_ID?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, COMMENT_ID), target)
    }

    @Test
    fun `GIVEN a link with a trailing slash WHEN reading it THEN gets the bill and the comment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID/?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, COMMENT_ID), target)
    }

    @Test
    fun `GIVEN other query params around the comment WHEN reading the link THEN gets the comment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID?utm_source=whatsapp&comentario=$COMMENT_ID&ref=share"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, COMMENT_ID), target)
    }

    @Test
    fun `GIVEN a link with a fragment WHEN reading it THEN gets the comment without the fragment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID?comentario=$COMMENT_ID#comments"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, COMMENT_ID), target)
    }

    @Test
    fun `GIVEN spaces around a pasted link WHEN reading it THEN gets the bill and the comment`() {
        // Given
        val url = "  https://blitzsplit.com/b/$BILL_ID?comentario=$COMMENT_ID \n"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, COMMENT_ID), target)
    }

    @Test
    fun `GIVEN a bill link without a comment WHEN reading it THEN gets the bill and no comment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, null), target)
    }

    @Test
    fun `GIVEN a bill link with only a fragment WHEN reading it THEN gets the bill and no comment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID#comments"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, null), target)
    }

    @Test
    fun `GIVEN a query without the comment WHEN reading the link THEN gets the bill and no comment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID?utm_source=whatsapp"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, null), target)
    }

    @Test
    fun `GIVEN an empty comment WHEN reading the link THEN gets the bill and no comment`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID?comentario="

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertEquals(BillCommentTarget(BILL_ID, null), target)
    }

    @Test
    fun `GIVEN a bill share link WHEN reading it as a comment link THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a group invite link WHEN reading it as a comment link THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/g/7Hk2pQ8x"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a path segment after the bill WHEN reading the link THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/b/$BILL_ID/edit?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a bill id too short to be a UUID WHEN reading the link THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/b/bill-1?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a bill id of 36 hex digits without the UUID dashes WHEN reading the link THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/b/0b6c1f0e8f5a4d7e9c2a3e1f5d7b9a11abcd?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a bill id with a letter outside hex WHEN reading the link THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/b/zb6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a link without the scheme WHEN reading it THEN finds none`() {
        // Given
        val url = "blitzsplit.com/b/$BILL_ID?comentario=$COMMENT_ID"

        // When
        val target = BillCommentLink.targetOf(url)

        // Then
        assertNull(target)
    }

    private companion object {
        const val BILL_ID = "0b6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11"
        const val COMMENT_ID = "5d2e7a10-3c4b-4f6a-8e9d-1a2b3c4d5e6f"
    }
}
