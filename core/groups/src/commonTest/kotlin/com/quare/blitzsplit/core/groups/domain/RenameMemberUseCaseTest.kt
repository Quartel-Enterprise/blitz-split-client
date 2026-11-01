package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.RenameMemberUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RenameMemberUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: RenameMemberUseCase

    @Test
    fun `GIVEN a member of a group WHEN renaming them THEN writes the new name at the current time`() = runTest {
        // Given
        prepareScenario(now = 2_000L)

        // When
        val result = useCase(
            memberId = "m-2",
            name = "Ana Paula",
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("m-2" to "Ana Paula"), repository.renamedMembers)
        assertEquals(listOf(2_000L), repository.memberChangeTimes)
    }

    @Test
    fun `GIVEN a name with spaces around WHEN renaming a member THEN writes it trimmed`() = runTest {
        // Given
        prepareScenario()
        val name = "  Ana Paula "

        // When
        useCase(
            memberId = "m-2",
            name = name,
        )

        // Then
        assertEquals(listOf("m-2" to "Ana Paula"), repository.renamedMembers)
    }

    @Test
    fun `GIVEN a long name with spaces around WHEN renaming a member THEN writes its first 40 letters trimmed`() =
        runTest {
            // Given
            prepareScenario()
            val name = "  " + "A".repeat(45)

            // When
            useCase(
                memberId = "m-2",
                name = name,
            )

            // Then
            assertEquals(listOf("m-2" to "A".repeat(40)), repository.renamedMembers)
        }

    @Test
    fun `GIVEN the device fails to write WHEN renaming a member THEN returns that failure`() = runTest {
        // Given
        prepareScenario(memberChangeError = IllegalStateException("disk full"))

        // When
        val result = useCase(
            memberId = "m-2",
            name = "Ana Paula",
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.renamedMembers.isEmpty())
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
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun prepareScenario(
        now: Long = 0L,
        memberChangeError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(listOf(trip)).also { it.memberChangeError = memberChangeError }
        useCase = RenameMemberUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
