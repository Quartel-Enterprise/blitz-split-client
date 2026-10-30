package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MembersMemberTest {
    @Test
    fun `GIVEN someone who has not joined WHEN reading the member THEN can be removed and invited`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
        )

        // When
        val canBeRemovedAndInvited = member.canBeRemoved to member.canBeInvited

        // Then
        assertTrue(canBeRemovedAndInvited.first)
        assertTrue(canBeRemovedAndInvited.second)
    }

    @Test
    fun `GIVEN someone who joined WHEN reading the member THEN can be neither removed nor invited`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = true,
        )

        // When
        val canBeRemovedAndInvited = member.canBeRemoved to member.canBeInvited

        // Then
        assertFalse(canBeRemovedAndInvited.first)
        assertFalse(canBeRemovedAndInvited.second)
    }

    @Test
    fun `GIVEN this device user WHEN reading the member THEN can be neither removed nor invited`() {
        // Given
        val member = member(
            isMe = true,
            hasJoined = true,
        )

        // When
        val canBeRemovedAndInvited = member.canBeRemoved to member.canBeInvited

        // Then
        assertFalse(canBeRemovedAndInvited.first)
        assertFalse(canBeRemovedAndInvited.second)
    }

    @Test
    fun `GIVEN this device user before a sync WHEN reading the member THEN can be neither removed nor invited`() {
        // Given
        val member = member(
            isMe = true,
            hasJoined = false,
        )

        // When
        val canBeRemovedAndInvited = member.canBeRemoved to member.canBeInvited

        // Then
        assertFalse(canBeRemovedAndInvited.first)
        assertFalse(canBeRemovedAndInvited.second)
    }

    private fun member(
        isMe: Boolean,
        hasJoined: Boolean,
    ): MembersMember = MembersMember(
        id = "member-2",
        name = "Ana",
        initials = "AN",
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = hasJoined,
        personalLink = null,
    )
}
