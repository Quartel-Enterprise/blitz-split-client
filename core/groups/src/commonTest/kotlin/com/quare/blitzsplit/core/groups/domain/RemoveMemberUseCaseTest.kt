package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RemoveMemberUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoveMemberUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: RemoveMemberUseCase

    @Test
    fun `GIVEN a member who has not joined WHEN removing them THEN removes that member at the current time`() =
        runTest {
            // Given
            prepareScenario(now = 3_000L)

            // When
            val result = useCase(memberId = "m-2")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(listOf("m-2"), repository.removedMemberIds)
            assertEquals(listOf(3_000L), repository.memberChangeTimes)
        }

    @Test
    fun `GIVEN the device fails to write WHEN removing a member THEN returns that failure`() = runTest {
        // Given
        prepareScenario(memberChangeError = IllegalStateException("disk full"))

        // When
        val result = useCase(memberId = "m-2")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.removedMemberIds.isEmpty())
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
                userId = null,
                inviteCode = null,
            ),
            GroupMember(
                id = "m-2",
                name = "Ana",
                avatarColor = AvatarColor.GREEN,
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        updatedAt = 0L,
    )

    private fun prepareScenario(
        now: Long = 0L,
        memberChangeError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(listOf(trip)).also { it.memberChangeError = memberChangeError }
        useCase = RemoveMemberUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
