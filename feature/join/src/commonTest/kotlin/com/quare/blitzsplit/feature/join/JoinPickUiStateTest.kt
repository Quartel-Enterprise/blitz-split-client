package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiState
import com.quare.blitzsplit.feature.join.presentation.model.NewMemberDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoinPickUiStateTest {
    @Test
    fun `GIVEN Pierre and Ana joined WHEN splitting the list THEN Bruno and Carla can be picked`() {
        // Given
        val state = state()

        // When
        val split = state.notJoined.map { it.name } to state.joined.map { it.name }

        // Then
        assertEquals(listOf("Bruno", "Carla") to listOf("Pierre", "Ana"), split)
    }

    @Test
    fun `GIVEN Bruno picked WHEN reading the selection THEN names Bruno`() {
        // Given
        val state = state(selectedMemberId = "member-3")

        // When
        val name = state.selectedName

        // Then
        assertEquals("Bruno", name)
    }

    @Test
    fun `GIVEN a picked member no longer in the list WHEN reading the selection THEN names nobody`() {
        // Given
        val state = state(selectedMemberId = "member-9")

        // When
        val name = state.selectedName

        // Then
        assertNull(name)
    }

    @Test
    fun `GIVEN nobody picked WHEN checking the join button THEN it is off`() {
        // Given
        val state = state(selectedMemberId = null)

        // When
        val canJoin = state.canJoin

        // Then
        assertFalse(canJoin)
    }

    @Test
    fun `GIVEN Bruno picked WHEN checking the join button THEN it is on`() {
        // Given
        val state = state(selectedMemberId = "member-3")

        // When
        val canJoin = state.canJoin

        // Then
        assertTrue(canJoin)
    }

    @Test
    fun `GIVEN Bruno picked and a join on its way WHEN checking the join button THEN it is off`() {
        // Given
        val state = state(
            selectedMemberId = "member-3",
            isJoining = true,
        )

        // When
        val canJoin = state.canJoin

        // Then
        assertFalse(canJoin)
    }

    @Test
    fun `GIVEN no new member draft WHEN checking the new member button THEN it is off`() {
        // Given
        val state = state(newMember = null)

        // When
        val canJoin = state.canJoinAsNewMember

        // Then
        assertFalse(canJoin)
    }

    @Test
    fun `GIVEN a free new name WHEN checking the new member button THEN it is on`() {
        // Given
        val state = state(
            newMember = NewMemberDraft(
                name = "Daniel",
                isNameTaken = false,
            ),
        )

        // When
        val canJoin = state.canJoinAsNewMember

        // Then
        assertTrue(canJoin)
    }

    @Test
    fun `GIVEN a free new name and a join on its way WHEN checking the new member button THEN it is off`() {
        // Given
        val state = state(
            newMember = NewMemberDraft(
                name = "Daniel",
                isNameTaken = false,
            ),
            isJoining = true,
        )

        // When
        val canJoin = state.canJoinAsNewMember

        // Then
        assertFalse(canJoin)
    }

    @Test
    fun `GIVEN a taken new name WHEN checking the new member button THEN it is off`() {
        // Given
        val state = state(
            newMember = NewMemberDraft(
                name = "Ana",
                isNameTaken = true,
            ),
        )

        // When
        val canJoin = state.canJoinAsNewMember

        // Then
        assertFalse(canJoin)
    }

    private val members = listOf(
        member("member-1", "Pierre", hasJoined = true),
        member("member-2", "Ana", hasJoined = true),
        member("member-3", "Bruno", hasJoined = false),
        member("member-4", "Carla", hasJoined = false),
    )

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

    private fun state(
        selectedMemberId: String? = null,
        newMember: NewMemberDraft? = null,
        isJoining: Boolean = false,
    ): JoinPickUiState = JoinPickUiState(
        isLoaded = true,
        members = members,
        selectedMemberId = selectedMemberId,
        takenName = null,
        creatorName = "Pierre",
        newMember = newMember,
        isJoining = isJoining,
    )
}
