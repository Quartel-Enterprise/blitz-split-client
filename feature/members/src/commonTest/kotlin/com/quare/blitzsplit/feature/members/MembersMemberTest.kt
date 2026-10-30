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

    @Test
    fun `GIVEN someone not joined with a personal link WHEN reading the member THEN the personal invite is ready`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            personalLink = CARLA_LINK,
        )

        // When
        val isReady = member.isPersonalInviteReady

        // Then
        assertTrue(isReady)
    }

    @Test
    fun `GIVEN someone not joined without a link yet WHEN reading the member THEN the personal invite is not ready`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            personalLink = null,
        )

        // When
        val isReady = member.isPersonalInviteReady

        // Then
        assertFalse(isReady)
    }

    @Test
    fun `GIVEN someone who joined with a link WHEN reading the member THEN the personal invite is not ready`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = true,
            personalLink = CARLA_LINK,
        )

        // When
        val isReady = member.isPersonalInviteReady

        // Then
        assertFalse(isReady)
    }

    @Test
    fun `GIVEN this device user with a link WHEN reading the member THEN the personal invite is not ready`() {
        // Given
        val member = member(
            isMe = true,
            hasJoined = false,
            personalLink = CARLA_LINK,
        )

        // When
        val isReady = member.isPersonalInviteReady

        // Then
        assertFalse(isReady)
    }

    private fun member(
        isMe: Boolean,
        hasJoined: Boolean,
        personalLink: String? = null,
    ): MembersMember = MembersMember(
        id = "member-2",
        name = "Ana",
        initials = "AN",
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = hasJoined,
        personalLink = personalLink,
    )

    private companion object {
        const val CARLA_LINK = "https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z?from=p1Er2e"
    }
}
