package com.quare.blitzsplit.core.profile.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileLinkTest {
    @Test
    fun `GIVEN a code WHEN building its link THEN points to the blitzsplit profile path`() {
        // Given
        val code = CODE

        // When
        val link = ProfileLink.of(code)

        // Then
        assertEquals("https://blitzsplit.com/u/$CODE", link)
    }

    @Test
    fun `GIVEN a link built from a code WHEN reading its code THEN gives the same code back`() {
        // Given
        val link = ProfileLink.of(CODE)

        // When
        val code = ProfileLink.codeOf(link)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link on another host over http WHEN reading its code THEN gives the code`() {
        // Given
        val url = "http://localhost:5173/u/$CODE"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link on the Portuguese domain WHEN reading its code THEN gives the code`() {
        // Given
        val url = "https://borarachar.com/u/$CODE"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with a trailing slash WHEN reading its code THEN gives the code`() {
        // Given
        val url = "https://blitzsplit.com/u/$CODE/"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with a query and a fragment WHEN reading its code THEN gives the code`() {
        // Given
        val url = "https://blitzsplit.com/u/$CODE/?utm_source=whatsapp#top"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a link with only a fragment WHEN reading its code THEN gives the code`() {
        // Given
        val url = "https://blitzsplit.com/u/$CODE#top"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a pasted link with spaces around it WHEN reading its code THEN gives the code`() {
        // Given
        val url = "  https://blitzsplit.com/u/$CODE \n"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertEquals(CODE, code)
    }

    @Test
    fun `GIVEN a group invite link WHEN reading a profile code THEN gives none`() {
        // Given
        val url = "https://blitzsplit.com/g/$CODE"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code one character short WHEN reading the link THEN gives none`() {
        // Given
        val url = "https://blitzsplit.com/u/${CODE.dropLast(1)}"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code one character long WHEN reading the link THEN gives none`() {
        // Given
        val url = "https://blitzsplit.com/u/${CODE}b"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code with a symbol WHEN reading the link THEN gives none`() {
        // Given
        val url = "https://blitzsplit.com/u/7Hk2-Q8xZa"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link with a path after the code WHEN reading its code THEN gives none`() {
        // Given
        val url = "https://blitzsplit.com/u/$CODE/edit"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link without a scheme WHEN reading its code THEN gives none`() {
        // Given
        val url = "blitzsplit.com/u/$CODE"

        // When
        val code = ProfileLink.codeOf(url)

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link WHEN showing it THEN drops the scheme`() {
        // Given
        val link = ProfileLink.of(CODE)

        // When
        val shown = ProfileLink.displayOf(link)

        // Then
        assertEquals("blitzsplit.com/u/$CODE", shown)
    }

    @Test
    fun `GIVEN ten letters and digits WHEN checking the code THEN it is a code`() {
        // Given
        val code = CODE

        // When
        val isCode = ProfileLink.isCode(code)

        // Then
        assertTrue(isCode)
    }

    @Test
    fun `GIVEN nine letters and digits WHEN checking the code THEN it is not a code`() {
        // Given
        val code = CODE.dropLast(1)

        // When
        val isCode = ProfileLink.isCode(code)

        // Then
        assertFalse(isCode)
    }

    @Test
    fun `GIVEN ten characters with a symbol WHEN checking the code THEN it is not a code`() {
        // Given
        val code = "7Hk2pQ8x_a"

        // When
        val isCode = ProfileLink.isCode(code)

        // Then
        assertFalse(isCode)
    }

    @Test
    fun `GIVEN ten characters with an accented letter WHEN checking the code THEN it is not a code`() {
        // Given
        val code = "7Hk2pQ8xZá"

        // When
        val isCode = ProfileLink.isCode(code)

        // Then
        assertFalse(isCode)
    }

    private companion object {
        const val CODE = "7Hk2pQ8xZa"
    }
}
