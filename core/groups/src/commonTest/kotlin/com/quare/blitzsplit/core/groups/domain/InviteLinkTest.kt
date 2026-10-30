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

    @Test
    fun `GIVEN the link of a group WHEN showing it THEN drops the scheme`() {
        // Given
        val link = InviteLink.of("aB3dE5gH")

        // When
        val shown = InviteLink.displayOf(link)

        // Then
        assertEquals("blitzsplit.com/g/aB3dE5gH", shown)
    }

    @Test
    fun `GIVEN a personal link with whoever shared it WHEN showing it THEN drops the scheme and the sharer`() {
        // Given
        val link = InviteLink.personalOf(
            inviteCode = "aB3dE5gH",
            memberCode = "c3Rt9z",
            inviterCode = "Xy7kQ2",
        )

        // When
        val shown = InviteLink.displayOf(link)

        // Then
        assertEquals("blitzsplit.com/g/aB3dE5gH/c3Rt9z", shown)
    }

    @Test
    fun `GIVEN the message shared from Convidar WHEN finding the invite in it THEN is the member and who shares it`() {
        // Given
        val text = "Entra no grupo Viagem como Carla no Bora Rachar: " +
            "https://blitzsplit.com/g/AB12cd34/c3Rt9z?from=x1Y2z3"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = "c3Rt9z",
                inviterCode = "x1Y2z3",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN the link of a group alone WHEN finding the invite in it THEN is the group with nobody else`() {
        // Given
        val text = "https://blitzsplit.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link without https WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "blitzsplit.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link without https WHEN finding the invite in it THEN reads the member and who shares it`() {
        // Given
        val text = "blitzsplit.com/g/AB12cd34/c3Rt9z?from=x1Y2z3"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = "c3Rt9z",
                inviterCode = "x1Y2z3",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link with nobody sharing it WHEN finding the invite in a text THEN is the member alone`() {
        // Given
        val text = "Entra como Carla: https://blitzsplit.com/g/AB12cd34/c3Rt9z"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = "c3Rt9z",
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link of plain http WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "http://blitzsplit.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link on a line of its own WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Entra no grupo Viagem\n\nhttps://blitzsplit.com/g/AB12cd34\n"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link ending a sentence with a period WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Entra pelo link https://blitzsplit.com/g/AB12cd34."

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link followed by a comma WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Entra pelo link https://blitzsplit.com/g/AB12cd34, combinado"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link followed by a semicolon WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Entra pelo link https://blitzsplit.com/g/AB12cd34; valeu"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link wrapped in parentheses WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Bora rachar (https://blitzsplit.com/g/AB12cd34)"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link closing a parenthesis WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Bora rachar (entra em https://blitzsplit.com/g/AB12cd34)"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link followed by an exclamation mark WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "Entra logo https://blitzsplit.com/g/AB12cd34!"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a personal link ending a sentence WHEN finding the invite in it THEN reads who shares it whole`() {
        // Given
        val text = "Entra como Carla: https://blitzsplit.com/g/AB12cd34/c3Rt9z?from=x1Y2z3."

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = "c3Rt9z",
                inviterCode = "x1Y2z3",
            ),
            target,
        )
    }

    @Test
    fun `GIVEN two invite links WHEN finding the invite in the text THEN the first one wins`() {
        // Given
        val text = "Primeiro https://blitzsplit.com/g/AB12cd34 depois https://blitzsplit.com/g/Zx9Yw8Vu/c3Rt9z"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN another URL before the invite WHEN finding the invite in the text THEN is the invite`() {
        // Given
        val text = "Veja https://example.com/menu e entra em https://blitzsplit.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a text with no link WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "Bora rachar a conta do jantar"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN an empty text WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = ""

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a link with a code of 7 characters WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "Entra: https://blitzsplit.com/g/AB12cd3"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a link with a code of 9 characters WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "Entra: https://blitzsplit.com/g/AB12cd345"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a personal link with a member code of 5 characters WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "https://blitzsplit.com/g/AB12cd34/c3Rt9"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a link to another page of the site WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "https://blitzsplit.com/x/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a link of another site WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "https://example.com/menu"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN another site with the path of an invite WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "https://example.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN another site with the path of an invite without https WHEN finding the invite THEN finds nothing`() {
        // Given
        val text = "Menu: example.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }

    @Test
    fun `GIVEN a link of the borarachar domain WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "https://borarachar.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN a link of www in capitals WHEN finding the invite in it THEN is the group`() {
        // Given
        val text = "WWW.BlitzSplit.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertEquals(
            InviteTarget(
                code = "AB12cd34",
                memberCode = null,
                inviterCode = null,
            ),
            target,
        )
    }

    @Test
    fun `GIVEN the app scheme with a group path WHEN finding the invite in it THEN finds nothing`() {
        // Given
        val text = "borarachar://blitzsplit.com/g/AB12cd34"

        // When
        val target = InviteLink.findIn(text)

        // Then
        assertNull(target)
    }
}
