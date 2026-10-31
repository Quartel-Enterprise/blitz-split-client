package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ChangeInviteLinkUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChangeInviteLinkUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: ChangeInviteLinkUseCase

    @Test
    fun `GIVEN a group the device user created WHEN changing its link THEN the group gets the new code`() = runTest {
        // Given
        prepareScenario(nextInviteCode = "Kp4mW9zQ")

        // When
        val result = useCase(groupId = "group-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("group-1"), repository.rotatedInviteCodeGroupIds)
        assertEquals(
            "Kp4mW9zQ",
            repository.groups.value
                .single()
                .inviteCode,
        )
    }

    @Test
    fun `GIVEN the server refuses WHEN changing the link THEN returns that failure and keeps the old code`() = runTest {
        // Given
        prepareScenario(rotateInviteCodeError = IllegalStateException("only the creator may change the link"))

        // When
        val result = useCase(groupId = "group-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(
            "aB3dE5gH",
            repository.groups.value
                .single()
                .inviteCode,
        )
    }

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = "m-1",
                name = "Pierre",
                avatarColor = AvatarColor.LILAC,
                isMe = true,
                hasJoined = true,
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = "aB3dE5gH",
        isCreatedByMe = true,
    )

    private fun prepareScenario(
        nextInviteCode: String = "newCode1",
        rotateInviteCodeError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(listOf(trip)).also {
            it.nextInviteCode = nextInviteCode
            it.rotateInviteCodeError = rotateInviteCodeError
        }
        useCase = ChangeInviteLinkUseCase(repository)
    }
}
