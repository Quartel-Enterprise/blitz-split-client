package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.viewmodel.toJoinMember
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinMemberMappingTest {
    @Test
    fun `GIVEN Ana who joined WHEN mapping her THEN keeps her id color and join with her first letter`() {
        // Given
        val ana = InviteMember(
            "member-2",
            "Ana",
            AvatarColor.PEACH,
            hasJoined = true,
            photoUrl = null,
        )

        // When
        val member = ana.toJoinMember()

        // Then
        assertEquals(
            JoinMember(
                "member-2",
                "Ana",
                "A",
                AvatarColor.PEACH,
                hasJoined = true,
                photoUrl = null,
            ),
            member,
        )
    }

    @Test
    fun `GIVEN Ana who joined with a profile photo WHEN mapping her THEN keeps the photo`() {
        // Given
        val ana = InviteMember(
            "member-2",
            "Ana",
            AvatarColor.PEACH,
            hasJoined = true,
            photoUrl = ANA_PHOTO,
        )

        // When
        val member = ana.toJoinMember()

        // Then
        assertEquals(ANA_PHOTO, member.photoUrl)
    }

    @Test
    fun `GIVEN a member with two names WHEN mapping it THEN the initials are the first letter of each`() {
        // Given
        val anaPaula = InviteMember(
            "member-5",
            "Ana Paula",
            AvatarColor.SAND,
            hasJoined = false,
            photoUrl = null,
        )

        // When
        val member = anaPaula.toJoinMember()

        // Then
        assertEquals("AP", member.initials)
    }

    private companion object {
        const val ANA_PHOTO = "https://cdn.blitzsplit.com/avatars/ana.jpg"
    }
}
