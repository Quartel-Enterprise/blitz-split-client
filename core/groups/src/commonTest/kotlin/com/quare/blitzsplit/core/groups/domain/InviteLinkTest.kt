package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InviteLinkTest {
    @Test
    fun `GIVEN the invite code of a group WHEN building its link THEN points to the group page of Blitz Split`() {
        // Given
        val inviteCode = "aB3dE5gH"

        // When
        val link = InviteLink.of(inviteCode)

        // Then
        assertEquals("https://blitzsplit.com/g/aB3dE5gH", link)
    }

    @Test
    fun `GIVEN a link to a group page of Blitz Split WHEN reading its code THEN returns the code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link of Bora Rachar WHEN reading its code THEN returns the code`() {
        // Given
        val url = "https://borarachar.com/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link of a local web server WHEN reading its code THEN returns the code`() {
        // Given
        val url = "http://localhost:5173/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link over plain http WHEN reading its code THEN returns the code`() {
        // Given
        val url = "http://blitzsplit.com/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link with a trailing slash WHEN reading its code THEN returns the code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link with a query WHEN reading its code THEN returns the code without the query`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH?utm_source=whatsapp"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link with a fragment WHEN reading its code THEN returns the code without the fragment`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH#join"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a link with a trailing slash and a query WHEN reading its code THEN returns the code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/?ref=qr"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN a code of digits only WHEN reading it from a link THEN returns the code`() {
        // Given
        val url = "https://blitzsplit.com/g/12345678"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("12345678", code)
    }

    @Test
    fun `GIVEN a link built for a code WHEN reading its code back THEN returns the same code`() {
        // Given
        val url = InviteLink.of("Zx9Yw8Vu")

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("Zx9Yw8Vu", code)
    }

    @Test
    fun `GIVEN a link to another page of the site WHEN reading its code THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/x/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link without the group path WHEN reading its code THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code of 7 characters WHEN reading it from a link THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5g"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code of 9 characters WHEN reading it from a link THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gHi"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code with symbols WHEN reading it from a link THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3-E5_H"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a code with an accented letter WHEN reading it from a link THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gÁ"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link with a path after the code WHEN reading its code THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/members"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link with a path before the group page WHEN reading its code THEN returns no code`() {
        // Given
        val url = "https://blitzsplit.com/app/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link without a scheme WHEN reading its code THEN returns no code`() {
        // Given
        val url = "blitzsplit.com/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link of another scheme WHEN reading its code THEN returns no code`() {
        // Given
        val url = "ftp://blitzsplit.com/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN the deep link of the browser sign in callback WHEN reading its code THEN returns no code`() {
        // Given
        val url = "borarachar://auth-callback#access_token=abc"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN the app scheme with a group path WHEN reading its code THEN returns no code`() {
        // Given
        val url = "borarachar://blitzsplit.com/g/aB3dE5gH"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }

    @Test
    fun `GIVEN a link with spaces and a line break around it WHEN reading its code THEN returns the code`() {
        // Given
        val url = "  https://blitzsplit.com/g/aB3dE5gH\n"

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertEquals("aB3dE5gH", code)
    }

    @Test
    fun `GIVEN an empty text WHEN reading its code THEN returns no code`() {
        // Given
        val url = ""

        // When
        val code = InviteLink.targetOf(url)?.code

        // Then
        assertNull(code)
    }
}
