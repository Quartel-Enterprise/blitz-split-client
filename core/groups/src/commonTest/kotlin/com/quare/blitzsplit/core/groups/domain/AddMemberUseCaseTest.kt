package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.AddMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AddMemberUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: AddMemberUseCase

    @Test
    fun `GIVEN a group on the device WHEN adding a person THEN adds someone who has not joined at the current time`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(trip),
                now = 1_000L,
            )

            // When
            val result = useCase(
                groupId = "group-1",
                name = "Bruno",
            )

            // Then
            assertTrue(result.isSuccess)
            val (groupId, added) = repository.addedMembers.single()
            assertEquals("group-1", groupId)
            assertEquals("Bruno", added.name)
            assertFalse(added.isMe)
            assertFalse(added.hasJoined)
            assertEquals(listOf(1_000L), repository.memberChangeTimes)
        }

    @Test
    fun `GIVEN a group on the device WHEN adding a person THEN the new member gets an id of its own`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))

        // When
        useCase(
            groupId = "group-1",
            name = "Bruno",
        )

        // Then
        val id = repository.addedMembers
            .single()
            .second.id
        assertTrue(uuid.matches(id))
        assertFalse(id in trip.members.map { it.id })
    }

    @Test
    fun `GIVEN a person already added WHEN adding another THEN each gets a different id`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))
        useCase(
            groupId = "group-1",
            name = "Bruno",
        )

        // When
        useCase(
            groupId = "group-1",
            name = "Carla",
        )

        // Then
        val ids = repository.addedMembers.map { it.second.id }
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `GIVEN a name with spaces around WHEN adding a person THEN stores it trimmed`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))
        val name = "  Bruno   "

        // When
        useCase(
            groupId = "group-1",
            name = name,
        )

        // Then
        assertEquals(
            "Bruno",
            repository.addedMembers
                .single()
                .second.name,
        )
    }

    @Test
    fun `GIVEN a name longer than 40 WHEN adding a person THEN stores its first 40 letters`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))
        val name = "B".repeat(40) + "runo"

        // When
        useCase(
            groupId = "group-1",
            name = name,
        )

        // Then
        assertEquals(
            "B".repeat(40),
            repository.addedMembers
                .single()
                .second.name,
        )
    }

    @Test
    fun `GIVEN a long name with spaces around WHEN adding a person THEN trims before capping at 40`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))
        val name = "   " + "B".repeat(40) + "   "

        // When
        useCase(
            groupId = "group-1",
            name = name,
        )

        // Then
        assertEquals(
            "B".repeat(40),
            repository.addedMembers
                .single()
                .second.name,
        )
    }

    @Test
    fun `GIVEN members in blue peach and green WHEN adding a person THEN gets lilac the first color nobody has`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        colors = listOf(AvatarColor.GREEN, AvatarColor.BLUE, AvatarColor.PEACH),
                    ),
                ),
            )

            // When
            useCase(
                groupId = "group-1",
                name = "Bruno",
            )

            // Then
            assertEquals(
                AvatarColor.LILAC,
                repository.addedMembers
                    .single()
                    .second.avatarColor,
            )
        }

    @Test
    fun `GIVEN every color taken and blue twice WHEN adding a person THEN gets peach the first of the fewest`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-1",
                        colors = listOf(
                            AvatarColor.BLUE,
                            AvatarColor.SAND,
                            AvatarColor.BLUE,
                            AvatarColor.LILAC,
                            AvatarColor.GREEN,
                            AvatarColor.PEACH,
                        ),
                    ),
                ),
            )

            // When
            useCase(
                groupId = "group-1",
                name = "Bruno",
            )

            // Then
            assertEquals(
                AvatarColor.PEACH,
                repository.addedMembers
                    .single()
                    .second.avatarColor,
            )
        }

    @Test
    fun `GIVEN another group uses the color this one lacks WHEN adding a person THEN only this group counts`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        id = "group-2",
                        colors = listOf(AvatarColor.LILAC, AvatarColor.LILAC, AvatarColor.LILAC),
                    ),
                    group(
                        id = "group-1",
                        colors = listOf(AvatarColor.BLUE, AvatarColor.PEACH, AvatarColor.GREEN),
                    ),
                ),
            )

            // When
            useCase(
                groupId = "group-1",
                name = "Bruno",
            )

            // Then
            assertEquals(
                AvatarColor.LILAC,
                repository.addedMembers
                    .single()
                    .second.avatarColor,
            )
        }

    @Test
    fun `GIVEN no group with that id on the device WHEN adding a person THEN fails and adds nobody`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))

        // When
        val result = useCase(
            groupId = "group-gone",
            name = "Bruno",
        )

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.addedMembers.isEmpty())
        assertTrue(repository.memberChangeTimes.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to store the member WHEN adding a person THEN returns that failure`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(trip),
            memberChangeError = IllegalStateException("disk full"),
        )

        // When
        val result = useCase(
            groupId = "group-1",
            name = "Bruno",
        )

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.addedMembers.isEmpty())
    }

    private fun group(
        id: String,
        colors: List<AvatarColor>,
    ): Group = Group(
        id = id,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        members = colors.mapIndexed { index, color ->
            GroupMember(
                id = "$id-m-$index",
                name = "Pessoa $index",
                avatarColor = color,
                isMe = index == 0,
                hasJoined = index == 0,
            )
        },
        isPending = false,
        inviteCode = null,
    )

    private val trip = group(
        id = "group-1",
        colors = listOf(AvatarColor.LILAC, AvatarColor.GREEN),
    )
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    private fun prepareScenario(
        storedGroups: List<Group>,
        now: Long = 0L,
        memberChangeError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(storedGroups).also { it.memberChangeError = memberChangeError }
        useCase = AddMemberUseCase(
            repository = repository,
            observeGroup = ObserveGroupUseCase(repository),
            currentTimeMillis = { now },
        )
    }
}
