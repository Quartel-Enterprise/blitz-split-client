package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class MembersUiStateTest {
    @Test
    fun `GIVEN four members and two joined WHEN counting THEN there are four members and two joined`() {
        // Given
        val state = state(
            members = listOf(
                member(
                    isMe = true,
                    hasJoined = true,
                ),
                member(hasJoined = false),
                member(hasJoined = true),
                member(hasJoined = false),
            ),
        )

        // When
        val counts = state.memberCount to state.joinedCount

        // Then
        assertEquals(4 to 2, counts)
    }

    @Test
    fun `GIVEN nothing loaded WHEN counting THEN there are no members and none joined`() {
        // Given
        val state = state(members = emptyList())

        // When
        val counts = state.memberCount to state.joinedCount

        // Then
        assertEquals(0 to 0, counts)
    }

    private fun state(members: List<MembersMember> = emptyList()): MembersUiState = MembersUiState(
        groupName = "Viagem Floripa",
        currency = GroupCurrency.BRL,
        displayLink = null,
        members = members,
        editor = null,
        memberToRemove = null,
        memberToRelease = null,
        isReleasing = false,
        isLoaded = true,
        homeTab = HomeTab.GROUPS,
        personalInvite = null,
        myBalance = null,
        isEveryoneSettled = false,
        recordedPayment = null,
    )

    private fun member(
        isMe: Boolean = false,
        hasJoined: Boolean,
    ): MembersMember = MembersMember(
        id = "member",
        name = "Ana",
        initials = "AN",
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = hasJoined,
        personalLink = null,
        balance = null,
        canBeReleased = false,
    )
}
