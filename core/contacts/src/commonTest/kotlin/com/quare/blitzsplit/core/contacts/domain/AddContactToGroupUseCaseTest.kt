package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.contact
import com.quare.blitzsplit.core.contacts.contactGroup
import com.quare.blitzsplit.core.contacts.domain.model.MemberNameTakenException
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.AddContactToGroupUseCase
import com.quare.blitzsplit.core.contacts.group
import com.quare.blitzsplit.core.contacts.me
import com.quare.blitzsplit.core.contacts.member
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.AddMemberUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.CheckMemberNameUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddContactToGroupUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: AddContactToGroupUseCase

    @Test
    fun `GIVEN a guest WHEN adding them to a group THEN adds their name as someone who has not joined`() = runTest {
        // Given
        prepareScenario(now = 1_000L)

        // When
        val result = useCase(diego, groupId = "ape")

        // Then
        assertTrue(result.isSuccess)
        val (groupId, added) = repository.addedMembers.single()
        assertEquals("ape", groupId)
        assertEquals("Diego", added.name)
        assertFalse(added.hasJoined)
        assertFalse(added.isMe)
        assertNull(added.userId)
        assertEquals(listOf(1_000L), repository.memberChangeTimes)
    }

    @Test
    fun `GIVEN someone with an account WHEN adding them to a group THEN adds them already joined as their account`() =
        runTest {
            // Given
            prepareScenario(now = 1_000L)

            // When
            val result = useCase(carla, groupId = "ape")

            // Then
            assertTrue(result.isSuccess)
            val (groupId, added) = repository.addedMembers.single()
            assertEquals("ape", groupId)
            assertEquals("Carla", added.name)
            assertTrue(added.hasJoined)
            assertFalse(added.isMe)
            assertEquals("carla", added.userId)
            assertEquals(AvatarColor.PEACH, added.avatarColor)
            assertEquals(listOf(1_000L), repository.memberChangeTimes)
        }

    @Test
    fun `GIVEN their color taken in the group WHEN adding someone with an account THEN gets the least used one`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(
                        "ape",
                        me("ape"),
                        member("m1", "Bruno", color = AvatarColor.PEACH),
                        member("m2", "Elisa", color = AvatarColor.BLUE),
                        member("m3", "Felipe", color = AvatarColor.GREEN),
                    ),
                ),
            )

            // When
            useCase(carla, groupId = "ape")

            // Then
            assertEquals(
                AvatarColor.SAND,
                repository.addedMembers
                    .single()
                    .second.avatarColor,
            )
        }

    @Test
    fun `GIVEN another member called Carla WHEN adding the account Carla THEN adds her with a nickname`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group("ape", me("ape"), member("carla-ape", "Carla"))))

        // When
        val result = useCase(carla, groupId = "ape")

        // Then
        assertTrue(result.isSuccess)
        val added = repository.addedMembers.single().second
        assertEquals("Carla 2", added.name)
        assertEquals("carla", added.userId)
    }

    @Test
    fun `GIVEN an account called Diego in the group WHEN adding the guest Diego THEN fails and adds nobody`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(group("ape", me("ape"), member("diego-ape", "Diego", userId = "diego"))),
            )

            // When
            val result = useCase(diego, groupId = "ape")

            // Then
            assertIs<MemberNameTakenException>(result.exceptionOrNull())
            assertTrue(repository.addedMembers.isEmpty())
        }

    @Test
    fun `GIVEN someone with an account already in the group WHEN adding them THEN adds nobody`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group("ape", me("ape"), member("carla-ape", "Carla", userId = "carla"))),
        )

        // When
        val result = useCase(carla, groupId = "ape")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.addedMembers.isEmpty())
    }

    @Test
    fun `GIVEN a guest already in the group by that name WHEN adding them THEN adds nobody`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group("ape", me("ape"), member("diego-ape", "diego"))))

        // When
        val result = useCase(diego, groupId = "ape")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.addedMembers.isEmpty())
    }

    @Test
    fun `GIVEN no such group on the device WHEN adding someone with an account THEN fails and adds nobody`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(carla, groupId = "gone")

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.addedMembers.isEmpty())
    }

    @Test
    fun `GIVEN no such group on the device WHEN adding a guest THEN fails and adds nobody`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(diego, groupId = "gone")

        // Then
        assertTrue(result.isFailure)
        assertTrue(repository.addedMembers.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to store the member WHEN adding someone with an account THEN returns that failure`() =
        runTest {
            // Given
            prepareScenario(memberChangeError = IllegalStateException("disk full"))

            // When
            val result = useCase(carla, groupId = "ape")

            // Then
            assertIs<IllegalStateException>(result.exceptionOrNull())
            assertTrue(repository.addedMembers.isEmpty())
        }

    @Test
    fun `GIVEN the device fails to store the member WHEN adding a guest THEN returns that failure`() = runTest {
        // Given
        prepareScenario(memberChangeError = IllegalStateException("disk full"))

        // When
        val result = useCase(diego, groupId = "ape")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.addedMembers.isEmpty())
    }

    private val carla = contact(
        key = "user:carla",
        name = "Carla",
        userId = "carla",
        color = AvatarColor.PEACH,
        groups = listOf(contactGroup("floripa", "carla-floripa")),
    )
    private val diego = contact(
        key = "name:diego",
        name = "Diego",
        color = AvatarColor.GREEN,
        groups = listOf(contactGroup("floripa", "diego-floripa")),
    )

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(group("ape", me("ape"), member("bruno-ape", "Bruno", userId = "bruno"))),
        now: Long = 0L,
        memberChangeError: Throwable? = null,
    ) {
        repository = FakeGroupRepository(storedGroups).also { it.memberChangeError = memberChangeError }
        val observeGroup = ObserveGroupUseCase(repository)
        useCase = AddContactToGroupUseCase(
            repository = repository,
            observeGroup = observeGroup,
            addMember = AddMemberUseCase(repository, observeGroup) { now },
            checkMemberName = CheckMemberNameUseCase(),
            currentTimeMillis = { now },
        )
    }
}
