package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
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
            balance = null,
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

    @Test
    fun `GIVEN someone not joined who owes WHEN reading the member THEN has an open balance and cannot be removed`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            balance = MemberBalance(
                receivesMinor = 0,
                owesMinor = 11_800,
            ),
        )

        // When
        val openAndRemovable = member.hasOpenBalance to member.canBeRemoved

        // Then
        assertTrue(openAndRemovable.first)
        assertFalse(openAndRemovable.second)
    }

    @Test
    fun `GIVEN someone not joined who is owed WHEN reading the member THEN has an open balance and stays`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            balance = MemberBalance(
                receivesMinor = 22_350,
                owesMinor = 0,
            ),
        )

        // When
        val openAndRemovable = member.hasOpenBalance to member.canBeRemoved

        // Then
        assertTrue(openAndRemovable.first)
        assertFalse(openAndRemovable.second)
    }

    @Test
    fun `GIVEN someone not joined who owes as much as is owed WHEN reading the member THEN cannot be removed`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            balance = MemberBalance(
                receivesMinor = 5_000,
                owesMinor = 5_000,
            ),
        )

        // When
        val openAndRemovable = member.hasOpenBalance to member.canBeRemoved

        // Then
        assertTrue(openAndRemovable.first)
        assertFalse(openAndRemovable.second)
    }

    @Test
    fun `GIVEN someone not joined and settled WHEN reading the member THEN has no open balance and can be removed`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            balance = MemberBalance(
                receivesMinor = 0,
                owesMinor = 0,
            ),
        )

        // When
        val openAndRemovable = member.hasOpenBalance to member.canBeRemoved

        // Then
        assertFalse(openAndRemovable.first)
        assertTrue(openAndRemovable.second)
    }

    @Test
    fun `GIVEN a group without bills WHEN reading the member THEN has no open balance`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            balance = null,
        )

        // When
        val hasOpenBalance = member.hasOpenBalance

        // Then
        assertFalse(hasOpenBalance)
    }

    @Test
    fun `GIVEN someone who joined and owes WHEN reading the member THEN cannot be removed`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = true,
            balance = MemberBalance(
                receivesMinor = 0,
                owesMinor = 11_800,
            ),
        )

        // When
        val canBeRemoved = member.canBeRemoved

        // Then
        assertFalse(canBeRemoved)
    }

    @Test
    fun `GIVEN someone not joined who owes WHEN reading the member THEN can still be invited`() {
        // Given
        val member = member(
            isMe = false,
            hasJoined = false,
            personalLink = CARLA_LINK,
            balance = MemberBalance(
                receivesMinor = 0,
                owesMinor = 11_800,
            ),
        )

        // When
        val canBeInvited = member.canBeInvited to member.isPersonalInviteReady

        // Then
        assertTrue(canBeInvited.first)
        assertTrue(canBeInvited.second)
    }

    private fun member(
        isMe: Boolean,
        hasJoined: Boolean,
        personalLink: String? = null,
        balance: MemberBalance? = null,
    ): MembersMember = MembersMember(
        id = "member-2",
        name = "Ana",
        initials = "AN",
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = hasJoined,
        personalLink = personalLink,
        balance = balance,
        canBeReleased = false,
    )

    private companion object {
        const val CARLA_LINK = "https://blitzsplit.com/g/7Hk2pQ8x/c3Rt9z?from=p1Er2e"
    }
}
