package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
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

    @Test
    fun `GIVEN a member code and who shares it WHEN building the personal link THEN ends in who shares it`() {
        // Given
        val inviteCode = "aB3dE5gH"
        val memberCode = "Xy7kQ2"
        val inviterCode = "Pq9Rs4"

        // When
        val link = InviteLink.personalOf(
            inviteCode = inviteCode,
            memberCode = memberCode,
            inviterCode = inviterCode,
        )

        // Then
        assertEquals("https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4", link)
    }

    @Test
    fun `GIVEN a member code and nobody known sharing it WHEN building the personal link THEN has no query`() {
        // Given
        val inviteCode = "aB3dE5gH"
        val memberCode = "Xy7kQ2"

        // When
        val link = InviteLink.personalOf(
            inviteCode = inviteCode,
            memberCode = memberCode,
            inviterCode = null,
        )

        // Then
        assertEquals("https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2", link)
    }

    @Test
    fun `GIVEN the link of a group WHEN reading what it opens THEN is the group with no member and no sharer`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link WHEN reading what it opens THEN is the group and that member`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with who shares it WHEN reading what it opens THEN is the member and who shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = "Pq9Rs4",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link of another host WHEN reading what it opens THEN is the group and that member`() {
        // Given
        val url = "http://localhost:5173/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = "Pq9Rs4",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with a trailing slash WHEN reading what it opens THEN is the group and that member`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2/"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with a trailing slash and who shares it WHEN reading what it opens THEN reads both`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2/?from=Pq9Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = "Pq9Rs4",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with a fragment WHEN reading what it opens THEN is the member without the fragment`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2#join"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with who shares it and a fragment WHEN reading what it opens THEN reads both`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4#join"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = "Pq9Rs4",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with spaces around it WHEN reading what it opens THEN reads both codes`() {
        // Given
        val url = "  https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4\n"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = "Pq9Rs4",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link built with who shares it WHEN reading it back THEN returns the same codes`() {
        // Given
        val url = InviteLink.personalOf(
            inviteCode = "aB3dE5gH",
            memberCode = "Xy7kQ2",
            inviterCode = "Pq9Rs4",
        )

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = "Pq9Rs4",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link built without who shares it WHEN reading it back THEN returns the member alone`() {
        // Given
        val url = InviteLink.personalOf(
            inviteCode = "aB3dE5gH",
            memberCode = "Xy7kQ2",
            inviterCode = null,
        )

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = "Xy7kQ2",
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN who shares it among other parameters WHEN reading the personal link THEN returns who shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?utm_source=whatsapp&from=Pq9Rs4&ref=qr"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals("Pq9Rs4", target?.inviterCode)
    }

    @Test
    fun `GIVEN who shares it before other parameters WHEN reading the personal link THEN returns who shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4&utm_source=whatsapp"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals("Pq9Rs4", target?.inviterCode)
    }

    @Test
    fun `GIVEN a personal link with other parameters only WHEN reading it THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?utm_source=whatsapp"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN the link of a group with who shares it WHEN reading what it opens THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH?from=Pq9Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals(
            InviteTarget(
                code = "aB3dE5gH",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN who shares it with 5 characters WHEN reading the personal link THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN who shares it with 7 characters WHEN reading the personal link THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq9Rs4t"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN who shares it with a symbol WHEN reading the personal link THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq-Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN an empty code of who shares it WHEN reading the personal link THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from="

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN another parameter ending in from WHEN reading the personal link THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?xfrom=Pq9Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN who shares it in the fragment WHEN reading the personal link THEN nobody shares it`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2#from=Pq9Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target?.inviterCode)
    }

    @Test
    fun `GIVEN who shares it with a symbol WHEN reading the personal link THEN still opens that member`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2?from=Pq-Rs4"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertEquals("Xy7kQ2", target?.memberCode)
    }

    @Test
    fun `GIVEN a member code of 5 characters WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a member code of 7 characters WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2z"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a member code with a symbol WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7-Q2"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a member code with an accented letter WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQÁ"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a personal link with a path after the member WHEN reading what it opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH/Xy7kQ2/members"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN an empty segment before the member WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/g/aB3dE5gH//Xy7kQ2"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a member code on another page of the site WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "https://blitzsplit.com/x/aB3dE5gH/Xy7kQ2"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN the app scheme with a personal path WHEN reading what the link opens THEN opens nothing`() {
        // Given
        val url = "borarachar://blitzsplit.com/g/aB3dE5gH/Xy7kQ2"

        // When
        val target = InviteLink.targetOf(url)

        // Then
        assertNull(target)
    }
}
