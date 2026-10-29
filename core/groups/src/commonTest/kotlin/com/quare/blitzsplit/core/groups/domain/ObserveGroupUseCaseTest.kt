package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveGroupUseCaseTest {
    private lateinit var repository: FakeGroupRepository
    private lateinit var useCase: ObserveGroupUseCase
    private val emissions = mutableListOf<Group?>()

    @Test
    fun `GIVEN the group among others on the device WHEN observing it THEN emits that group`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(home, trip))

        // When
        val group = useCase("group-1").first()

        // Then
        assertEquals(trip, group)
    }

    @Test
    fun `GIVEN no group with that id on the device WHEN observing it THEN emits nothing found`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(home))

        // When
        val group = useCase("group-1").first()

        // Then
        assertNull(group)
    }

    @Test
    fun `GIVEN an observed group WHEN it leaves the device after a sign out THEN emits nothing found`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))
        collectGroup("group-1")

        // When
        repository.clear()

        // Then
        assertEquals(listOf<Group?>(trip, null), emissions)
    }

    @Test
    fun `GIVEN an observed group WHEN another group changes THEN does not emit again`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip, home))
        collectGroup("group-1")

        // When
        repository.groups.value = listOf(trip, home.copy(name = "Casa da praia"))

        // Then
        assertEquals(listOf<Group?>(trip), emissions)
    }

    @Test
    fun `GIVEN an observed group WHEN its invite code arrives THEN emits the group with the code`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(trip))
        collectGroup("group-1")
        val withCode = trip.copy(inviteCode = "aB3dE5gH")

        // When
        repository.groups.value = listOf(withCode)

        // Then
        assertEquals(listOf<Group?>(trip, withCode), emissions)
    }

    private fun TestScope.collectGroup(groupId: String) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase(groupId).toList(emissions) }
    }

    private val trip = Group(
        id = "group-1",
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = null,
    )
    private val home = Group(
        id = "group-2",
        name = "Casa",
        type = GroupType.HOME,
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = "Zx9Yw8Vu",
    )

    private fun prepareScenario(storedGroups: List<Group>) {
        repository = FakeGroupRepository(initialGroups = storedGroups)
        useCase = ObserveGroupUseCase(repository)
    }
}
