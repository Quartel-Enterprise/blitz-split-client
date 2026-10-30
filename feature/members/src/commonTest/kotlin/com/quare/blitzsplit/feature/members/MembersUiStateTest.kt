package com.quare.blitzsplit.feature.members

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.members.presentation.model.MembersMember
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `GIVEN just joined with an unprotected account WHEN checking the protect access invite THEN it shows`() {
        // Given
        val state = state(
            joinedAs = "Bruno",
            isAccountUnprotected = true,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertTrue(shows)
    }

    @Test
    fun `GIVEN just joined with a protected account WHEN checking the protect access invite THEN it hides`() {
        // Given
        val state = state(
            joinedAs = "Bruno",
            isAccountUnprotected = false,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN not opened from a join with an unprotected account WHEN checking the invite THEN it hides`() {
        // Given
        val state = state(
            joinedAs = null,
            isAccountUnprotected = true,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN not opened from a join with a protected account WHEN checking the invite THEN it hides`() {
        // Given
        val state = state(
            joinedAs = null,
            isAccountUnprotected = false,
        )

        // When
        val shows = state.showsProtectAccess

        // Then
        assertFalse(shows)
    }

    private fun state(
        members: List<MembersMember> = emptyList(),
        joinedAs: String? = null,
        isAccountUnprotected: Boolean = false,
    ): MembersUiState = MembersUiState(
        groupName = "Viagem Floripa",
        displayLink = null,
        members = members,
        editor = null,
        memberToRemove = null,
        isLoaded = true,
        joinedAs = joinedAs,
        isAccountUnprotected = isAccountUnprotected,
        personalInvite = null,
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
    )
}
