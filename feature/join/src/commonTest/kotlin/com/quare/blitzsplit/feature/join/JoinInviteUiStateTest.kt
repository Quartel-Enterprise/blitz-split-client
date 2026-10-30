package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteStatus
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiState
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinInviteUiStateTest {
    @Test
    fun `GIVEN four members WHEN counting THEN there are four members`() {
        // Given
        val state = state(members = listOf(pierre, ana, bruno, carla))

        // When
        val count = state.memberCount

        // Then
        assertEquals(4, count)
    }

    @Test
    fun `GIVEN Pierre and Bruno joined WHEN listing who joined THEN names them in the group order`() {
        // Given
        val state = state(members = listOf(pierre, ana, bruno, carla))

        // When
        val names = state.joinedNames

        // Then
        assertEquals(listOf("Pierre", "Bruno"), names)
    }

    @Test
    fun `GIVEN Pierre and Bruno joined WHEN listing the faces of who joined THEN leaves out the others`() {
        // Given
        val state = state(members = listOf(pierre, ana, bruno, carla))

        // When
        val joined = state.joined

        // Then
        assertEquals(listOf(pierre, bruno), joined)
    }

    @Test
    fun `GIVEN nothing loaded WHEN listing who joined THEN there are no members and no names`() {
        // Given
        val state = state(members = emptyList())

        // When
        val summary = state.memberCount to state.joinedNames

        // Then
        assertEquals(0 to emptyList<String>(), summary)
    }

    private val pierre = member("member-1", "Pierre", hasJoined = true)
    private val ana = member("member-2", "Ana", hasJoined = false)
    private val bruno = member("member-3", "Bruno", hasJoined = true)
    private val carla = member("member-4", "Carla", hasJoined = false)

    private fun member(
        id: String,
        name: String,
        hasJoined: Boolean,
    ): JoinMember = JoinMember(
        id = id,
        name = name,
        initials = name.take(2).uppercase(),
        avatarColor = AvatarColor.BLUE,
        hasJoined = hasJoined,
    )

    private fun state(members: List<JoinMember>): JoinInviteUiState = JoinInviteUiState(
        status = JoinInviteStatus.READY,
        groupName = "Viagem Floripa",
        groupInitials = "VF",
        groupColor = AvatarColor.GREEN,
        creatorName = "Pierre",
        members = members,
        displayLink = "blitzsplit.com/g/7Hk2pQxZ",
    )
}
