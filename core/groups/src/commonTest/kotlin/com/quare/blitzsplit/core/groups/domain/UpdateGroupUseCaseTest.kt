package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpdateGroupUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: UpdateGroupUseCase

    @Test
    fun `GIVEN a group WHEN editing it THEN writes that group at the current time`() = runTest {
        // Given
        prepareScenario(now = 3_000L)

        // When
        val result = useCase(
            groupId = "group-1",
            name = "Casa da praia",
            type = GroupType.HOME,
            color = AvatarColor.GREEN,
            currency = GroupCurrency.USD,
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("group-1"), repository.updatedGroupIds)
        assertEquals(listOf(3_000L), repository.groupUpdateTimes)
    }

    @Test
    fun `GIVEN a new type color and currency WHEN editing the group THEN writes them as chosen`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase(
            groupId = "group-1",
            name = "Viagem Floripa",
            type = GroupType.EVENT,
            color = AvatarColor.SAND,
            currency = GroupCurrency.EUR,
        )

        // Then
        val group = repository.groups.value.single()
        assertEquals(GroupType.EVENT, group.type)
        assertEquals(AvatarColor.SAND, group.color)
        assertEquals(GroupCurrency.EUR, group.currency)
    }

    @Test
    fun `GIVEN a name with spaces around WHEN editing the group THEN writes it trimmed`() = runTest {
        // Given
        prepareScenario()
        val name = "  Casa da praia "

        // When
        useCase(
            groupId = "group-1",
            name = name,
            type = GroupType.TRIP,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.BRL,
        )

        // Then
        assertEquals(
            "Casa da praia",
            repository.groups.value
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN a name over 40 letters WHEN editing the group THEN writes its first 40 letters`() = runTest {
        // Given
        prepareScenario()
        val name = "B".repeat(45)

        // When
        useCase(
            groupId = "group-1",
            name = name,
            type = GroupType.TRIP,
            color = AvatarColor.PEACH,
            currency = GroupCurrency.BRL,
        )

        // Then
        assertEquals(
            "B".repeat(40),
            repository.groups.value
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN a long name with spaces around WHEN editing the group THEN writes its first 40 letters trimmed`() =
        runTest {
            // Given
            prepareScenario()
            val name = "   " + "B".repeat(45)

            // When
            useCase(
                groupId = "group-1",
                name = name,
                type = GroupType.TRIP,
                color = AvatarColor.PEACH,
                currency = GroupCurrency.BRL,
            )

            // Then
            assertEquals(
                "B".repeat(40),
                repository.groups.value
                    .single()
                    .name,
            )
        }

    @Test
    fun `GIVEN the device fails to write WHEN editing the group THEN returns that failure`() = runTest {
        // Given
        prepareScenario(updateGroupError = IllegalStateException("disk full"))

        // When
        val result = useCase(
            groupId = "group-1",
            name = "Casa da praia",
            type = GroupType.HOME,
            color = AvatarColor.GREEN,
            currency = GroupCurrency.USD,
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(
            "Viagem Floripa",
            repository.groups.value
                .single()
                .name,
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
                userId = null,
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = "Xy7kQ2",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun prepareScenario(
        now: Long = 0L,
        updateGroupError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(listOf(trip)).also { it.updateGroupError = updateGroupError }
        useCase = UpdateGroupUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
