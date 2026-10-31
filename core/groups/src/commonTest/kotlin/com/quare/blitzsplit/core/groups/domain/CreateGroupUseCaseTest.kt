package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CreateGroupUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateGroupUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: CreateGroupUseCase

    @Test
    fun `GIVEN a name type color and currency WHEN creating the group THEN stores them pending at the current time`() =
        runTest {
            // Given
            prepareScenario(now = 1_000L)

            // When
            useCase(
                name = "Viagem Floripa",
                type = GroupType.TRIP,
                color = AvatarColor.PEACH,
                currency = GroupCurrency.BRL,
                members = listOf(me),
            )

            // Then
            val created = repository.createdGroups.single()
            assertEquals("Viagem Floripa", created.name)
            assertEquals(GroupType.TRIP, created.type)
            assertEquals(AvatarColor.PEACH, created.color)
            assertEquals(GroupCurrency.BRL, created.currency)
            assertTrue(created.isPending)
            assertEquals(listOf(1_000L), repository.createdAts)
        }

    @Test
    fun `GIVEN a new group WHEN creating it THEN returns the id it was stored with`() = runTest {
        // Given
        prepareScenario()

        // When
        val id = createGroup()

        // Then
        assertTrue(uuid.matches(id))
        assertEquals(id, repository.createdGroups.single().id)
    }

    @Test
    fun `GIVEN a group already created WHEN creating another THEN the new one gets an id of its own`() = runTest {
        // Given
        prepareScenario()
        val firstId = createGroup()

        // When
        val secondId = createGroup()

        // Then
        assertNotEquals(firstId, secondId)
    }

    @Test
    fun `GIVEN three members WHEN creating the group THEN each member gets an id of its own made on the device`() =
        runTest {
            // Given
            prepareScenario()
            val members = listOf(me, ana, bruno)

            // When
            createGroup(members = members)

            // Then
            val ids = repository.createdGroups
                .single()
                .members
                .map { it.id }
            assertTrue(ids.all(uuid::matches))
            assertEquals(3, ids.toSet().size)
            assertFalse(repository.createdGroups.single().id in ids)
        }

    @Test
    fun `GIVEN members in a given order WHEN creating the group THEN keeps that order`() = runTest {
        // Given
        prepareScenario()
        val members = listOf(me, bruno, ana)

        // When
        createGroup(members = members)

        // Then
        assertEquals(
            listOf("Pierre", "Bruno", "Ana"),
            repository.createdGroups
                .single()
                .members
                .map { it.name },
        )
    }

    @Test
    fun `GIVEN the creator and two names WHEN creating the group THEN only the creator is me and has joined`() =
        runTest {
            // Given
            prepareScenario()
            val members = listOf(me, ana, bruno)

            // When
            createGroup(members = members)

            // Then
            val created = repository.createdGroups.single().members
            assertEquals(listOf(true, false, false), created.map { it.isMe })
            assertEquals(listOf(true, false, false), created.map { it.hasJoined })
        }

    @Test
    fun `GIVEN members with their colors WHEN creating the group THEN keeps each color`() = runTest {
        // Given
        prepareScenario()
        val members = listOf(me, ana)

        // When
        createGroup(members = members)

        // Then
        assertEquals(
            listOf(AvatarColor.LILAC, AvatarColor.GREEN),
            repository.createdGroups
                .single()
                .members
                .map { it.avatarColor },
        )
    }

    @Test
    fun `GIVEN a name with spaces around WHEN creating the group THEN stores it trimmed`() = runTest {
        // Given
        prepareScenario()
        val name = "   Casa da Praia  "

        // When
        createGroup(name = name)

        // Then
        assertEquals("Casa da Praia", repository.createdGroups.single().name)
    }

    @Test
    fun `GIVEN a name longer than 40 WHEN creating the group THEN stores its first 40 letters`() = runTest {
        // Given
        prepareScenario()
        val name = "A".repeat(40) + "BCDE"

        // When
        createGroup(name = name)

        // Then
        assertEquals("A".repeat(40), repository.createdGroups.single().name)
    }

    @Test
    fun `GIVEN an emoji as the 40th letter WHEN creating the group THEN keeps the emoji whole`() = runTest {
        // Given
        prepareScenario()
        val name = "A".repeat(39) + "😀" + "B"

        // When
        createGroup(name = name)

        // Then
        assertEquals("A".repeat(39) + "😀", repository.createdGroups.single().name)
    }

    @Test
    fun `GIVEN a long name with spaces around WHEN creating the group THEN trims before capping at 40`() = runTest {
        // Given
        prepareScenario()
        val name = "   " + "A".repeat(40) + "   "

        // When
        createGroup(name = name)

        // Then
        assertEquals("A".repeat(40), repository.createdGroups.single().name)
    }

    @Test
    fun `GIVEN member names with spaces around WHEN creating the group THEN stores them trimmed`() = runTest {
        // Given
        prepareScenario()
        val members = listOf(me.copy(name = " Pierre "), ana.copy(name = "  Ana"))

        // When
        createGroup(members = members)

        // Then
        assertEquals(
            listOf("Pierre", "Ana"),
            repository.createdGroups
                .single()
                .members
                .map { it.name },
        )
    }

    @Test
    fun `GIVEN a member name longer than 40 WHEN creating the group THEN stores its first 40 letters`() = runTest {
        // Given
        prepareScenario()
        val members = listOf(me, ana.copy(name = "  " + "B".repeat(45)))

        // When
        createGroup(members = members)

        // Then
        assertEquals(
            "B".repeat(40),
            repository.createdGroups
                .single()
                .members[1]
                .name,
        )
    }

    @Test
    fun `GIVEN the device fails to store the group WHEN creating it THEN returns that failure and stores nothing`() =
        runTest {
            // Given
            prepareScenario(createError = IllegalStateException("disk full"))

            // When
            val result = useCase(
                name = "Viagem Floripa",
                type = GroupType.TRIP,
                color = AvatarColor.BLUE,
                currency = GroupCurrency.BRL,
                members = listOf(me, ana),
            )

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(repository.createdGroups.isEmpty())
            assertTrue(repository.groups.value.isEmpty())
        }

    @Test
    fun `GIVEN a new group WHEN creating it on the device THEN it has no invite code until the server makes one`() =
        runTest {
            // Given
            prepareScenario()

            // When
            createGroup()

            // Then
            assertNull(repository.createdGroups.single().inviteCode)
        }

    @Test
    fun `GIVEN a new group WHEN creating it on the device THEN it is created by me`() = runTest {
        // Given
        prepareScenario()

        // When
        createGroup()

        // Then
        assertTrue(repository.createdGroups.single().isCreatedByMe)
    }

    private suspend fun createGroup(
        name: String = "Viagem Floripa",
        members: List<NewGroupMember> = listOf(me),
    ): String = useCase(
        name = name,
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = members,
    ).getOrThrow()

    private val me = NewGroupMember(
        name = "Pierre",
        avatarColor = AvatarColor.LILAC,
        isMe = true,
    )
    private val ana = NewGroupMember(
        name = "Ana",
        avatarColor = AvatarColor.GREEN,
        isMe = false,
    )
    private val bruno = NewGroupMember(
        name = "Bruno",
        avatarColor = AvatarColor.SAND,
        isMe = false,
    )
    private val uuid = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    private fun prepareScenario(
        now: Long = 0L,
        createError: Throwable? = null,
    ) {
        repository = FakeGroupRepository().also { it.createError = createError }
        useCase = CreateGroupUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }
}
