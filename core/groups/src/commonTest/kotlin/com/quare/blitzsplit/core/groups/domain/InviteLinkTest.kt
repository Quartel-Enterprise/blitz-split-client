package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import kotlin.test.Test
import kotlin.test.assertEquals

class InviteLinkTest {
    @Test
    fun `GIVEN the invite code of a group WHEN building its link THEN points to the group page of Bora Rachar`() {
        // Given
        val inviteCode = "aB3dE5gH"

        // When
        val link = InviteLink.of(inviteCode)

        // Then
        assertEquals("https://borarachar.com/g/aB3dE5gH", link)
    }
}
