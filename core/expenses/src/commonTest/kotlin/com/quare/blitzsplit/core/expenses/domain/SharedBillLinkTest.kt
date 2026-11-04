package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedBillLinkTest {
    @Test
    fun `GIVEN a code WHEN building its link THEN is the code on blitzsplit dot com`() {
        // Given
        val code = CODE

        // When
        val link = SharedBillLink.of(code)

        // Then
        assertEquals("https://blitzsplit.com/c/7Hk2pQ8x", link)
    }

    @Test
    fun `GIVEN a bill share link WHEN reading its URL THEN is the link of its code`() {
        // Given
        val link = BillShareLink(
            code = CODE,
            visibility = BillLinkVisibility.MEMBERS,
        )

        // When
        val url = link.url

        // Then
        assertEquals("https://blitzsplit.com/c/7Hk2pQ8x", url)
    }

    @Test
    fun `GIVEN a link WHEN showing it THEN drops the scheme`() {
        // Given
        val link = "https://blitzsplit.com/c/7Hk2pQ8x"

        // When
        val shown = SharedBillLink.displayOf(link)

        // Then
        assertEquals("blitzsplit.com/c/7Hk2pQ8x", shown)
    }

    @Test
    fun `GIVEN an http link WHEN showing it THEN drops the scheme too`() {
        // Given
        val link = "http://localhost:5173/c/7Hk2pQ8x"

        // When
        val shown = SharedBillLink.displayOf(link)

        // Then
        assertEquals("localhost:5173/c/7Hk2pQ8x", shown)
    }

    @Test
    fun `GIVEN the link the app builds WHEN reading its code THEN gets the code back`() {
        // Given
        val url = SharedBillLink.of(CODE)

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link on another host over http WHEN reading its code THEN gets the code`() {
        // Given
        val url = "http://localhost:5173/c/7Hk2pQ8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link on borarachar dot com WHEN reading its code THEN gets the code`() {
        // Given
        val url = "https://borarachar.com/c/7Hk2pQ8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with a trailing slash WHEN reading its code THEN gets the code`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x/"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with a query and a fragment WHEN reading its code THEN gets the code`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x?utm_source=whatsapp#comments"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with a trailing slash and a query WHEN reading its code THEN gets the code`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x/?ref=share"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with only a fragment WHEN reading its code THEN gets the code`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x#comments"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN spaces around a pasted link WHEN reading its code THEN gets the code`() {
        // Given
        val url = "  https://blitzsplit.com/c/7Hk2pQ8x \n"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a group invite link WHEN reading a bill code THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/g/7Hk2pQ8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code of 7 characters WHEN reading it THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code of 9 characters WHEN reading it THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8xy"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code with a symbol WHEN reading it THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2-Q8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a path segment after the code WHEN reading it THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x/edit"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a path segment before the c WHEN reading the code THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/app/c/7Hk2pQ8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link without the scheme WHEN reading its code THEN finds none`() {
        // Given
        val url = "blitzsplit.com/c/7Hk2pQ8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link of another scheme WHEN reading its code THEN finds none`() {
        // Given
        val url = "ftp://blitzsplit.com/c/7Hk2pQ8x"

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN only the code WHEN reading it as a link THEN finds none`() {
        // Given
        val url = CODE

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code and a comment WHEN building the link THEN is the code link with the comment`() {
        // Given
        val code = CODE

        // When
        val link = SharedBillLink.of(code, COMMENT_ID)

        // Then
        assertEquals("https://blitzsplit.com/c/7Hk2pQ8x?comentario=$COMMENT_ID", link)
    }

    @Test
    fun `GIVEN the link the app builds at a comment WHEN reading its code THEN gets the code back`() {
        // Given
        val url = SharedBillLink.of(CODE, COMMENT_ID)

        // When
        val code = SharedBillLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN the link the app builds at a comment WHEN reading its comment THEN gets the comment back`() {
        // Given
        val url = SharedBillLink.of(CODE, COMMENT_ID)

        // When
        val commentId = SharedBillLink.commentIdOf(url)

        // Then
        assertEquals(COMMENT_ID, commentId)
    }

    @Test
    fun `GIVEN a link at a comment among other params and a fragment WHEN reading its comment THEN gets the comment`() {
        // Given
        val url = "http://localhost:5173/c/7Hk2pQ8x/?ref=share&comentario=$COMMENT_ID#comments"

        // When
        val commentId = SharedBillLink.commentIdOf(url)

        // Then
        assertEquals(COMMENT_ID, commentId)
    }

    @Test
    fun `GIVEN a link without a comment WHEN reading its comment THEN finds none`() {
        // Given
        val url = SharedBillLink.of(CODE)

        // When
        val commentId = SharedBillLink.commentIdOf(url)

        // Then
        assertNull(commentId)
    }

    @Test
    fun `GIVEN a link with other params only WHEN reading its comment THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/c/7Hk2pQ8x?utm_source=whatsapp"

        // When
        val commentId = SharedBillLink.commentIdOf(url)

        // Then
        assertNull(commentId)
    }

    @Test
    fun `GIVEN a group invite link with a comment WHEN reading its comment THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/g/7Hk2pQ8x?comentario=$COMMENT_ID"

        // When
        val commentId = SharedBillLink.commentIdOf(url)

        // Then
        assertNull(commentId)
    }

    @Test
    fun `GIVEN a comment link of a bill WHEN reading it as a bill share link comment THEN finds none`() {
        // Given
        val url = "https://blitzsplit.com/b/0b6c1f0e-8f5a-4d7e-9c2a-3e1f5d7b9a11?comentario=$COMMENT_ID"

        // When
        val commentId = SharedBillLink.commentIdOf(url)

        // Then
        assertNull(commentId)
    }

    private companion object {
        const val CODE = "7Hk2pQ8x"
        const val COMMENT_ID = "5d2e7a10-3c4b-4f6a-8e9d-1a2b3c4d5e6f"
    }
}
