package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.domain.model.ContactBookSnapshot
import com.quare.blitzsplit.core.contacts.domain.model.HiddenContact
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.ObserveContactBookUseCase
import com.quare.blitzsplit.core.contacts.group
import com.quare.blitzsplit.core.contacts.me
import com.quare.blitzsplit.core.contacts.meId
import com.quare.blitzsplit.core.contacts.member
import com.quare.blitzsplit.core.contacts.owes
import com.quare.blitzsplit.core.contacts.payment
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveContactBookUseCaseTest {
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var hidden: FakeHiddenContactRepository
    private lateinit var useCase: ObserveContactBookUseCase
    private val snapshots = mutableListOf<ContactBookSnapshot>()

    @Test
    fun `GIVEN groups with bills WHEN observing the contacts THEN lists the groups by their last activity`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(floripa(updatedAt = 1_000L), ape(updatedAt = 2_000L)),
            storedExpenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 3_000L)),
        )

        // When
        val snapshot = useCase().first()

        // Then
        assertEquals(listOf("floripa", "ape"), snapshot.groups.map { it.id })
        assertEquals(setOf("floripa"), snapshot.money.keys)
    }

    @Test
    fun `GIVEN Carla named differently in two groups WHEN observing THEN names her as the most recently used group`() =
        runTest {
            // Given
            prepareScenario(
                storedGroups = listOf(floripa(updatedAt = 1_000L), ape(updatedAt = 2_000L)),
                storedExpenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 3_000L)),
            )

            // When
            val snapshot = useCase().first()

            // Then
            val carla = snapshot.contacts.single()
            assertEquals("Carla", carla.name)
            assertEquals(listOf("floripa", "ape"), carla.groups.map { it.groupId })
            assertEquals(5_000L, carla.net.amountMinor)
        }

    @Test
    fun `GIVEN Carla hidden and settled WHEN observing THEN she is listed as hidden`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(floripa()),
            storedHidden = listOf(
                HiddenContact(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 200L,
                ),
            ),
        )

        // When
        val snapshot = useCase().first()

        // Then
        assertTrue(snapshot.contacts.single().isHidden)
    }

    @Test
    fun `GIVEN the contacts open WHEN hiding Carla THEN they show her hidden`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(floripa()))
        collectSnapshots()

        // When
        hidden.setHidden("user:carla", isHidden = true)
        runCurrent()

        // Then
        assertFalse(
            snapshots
                .first()
                .contacts
                .single()
                .isHidden,
        )
        assertTrue(
            snapshots
                .last()
                .contacts
                .single()
                .isHidden,
        )
    }

    @Test
    fun `GIVEN Carla hidden WHEN a bill with her arrives THEN she comes back`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(floripa()),
            storedHidden = listOf(
                HiddenContact(
                    "user:carla",
                    isHidden = true,
                    hiddenAt = 200L,
                ),
            ),
        )
        collectSnapshots()

        // When
        expenses.expenses.update {
            it + owes("floripa", "carla-floripa", meId("floripa"), 5_000L, updatedAt = 300L)
        }
        runCurrent()

        // Then
        val carla = snapshots.last().contacts.single()
        assertFalse(carla.isHidden)
        assertEquals(5_000L, carla.net.amountMinor)
    }

    @Test
    fun `GIVEN Carla owes you WHEN a payment of all of it arrives THEN the contacts show her settled`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(floripa()),
            storedExpenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L)),
        )
        collectSnapshots()

        // When
        settlements.settlements.update { it + payment("floripa", "carla-floripa", meId("floripa"), 5_000L) }
        runCurrent()

        // Then
        assertEquals(
            5_000L,
            snapshots
                .first()
                .contacts
                .single()
                .net.amountMinor,
        )
        assertTrue(
            snapshots
                .last()
                .contacts
                .single()
                .isSettled,
        )
    }

    @Test
    fun `GIVEN a snapshot WHEN looking Carla up by her key THEN finds her and nobody for an unknown key`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(floripa()))
        val snapshot = useCase().first()

        // When
        val found = snapshot.findOrNull("user:carla")

        // Then
        assertEquals("Carla", found?.name)
        assertEquals(null, snapshot.findOrNull("user:nobody"))
    }

    @Test
    fun `GIVEN a snapshot WHEN reading Carla history THEN reads it from the snapshot groups and money`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(floripa()),
            storedExpenses = listOf(owes("floripa", "carla-floripa", meId("floripa"), 5_000L, id = "dinner")),
        )
        val snapshot = useCase().first()
        val carla = snapshot.contacts.single()

        // When
        val history = snapshot.historyOf(carla)

        // Then
        assertEquals(listOf("dinner"), history.map { it.id })
    }

    private fun TestScope.collectSnapshots() {
        backgroundScope.launch { useCase().collect(snapshots::add) }
        runCurrent()
    }

    private fun floripa(updatedAt: Long = 0L): Group = group(
        "floripa",
        me("floripa"),
        member("carla-floripa", "Carla", userId = "carla"),
        updatedAt = updatedAt,
    )

    private fun ape(updatedAt: Long = 0L): Group = group(
        "ape",
        me("ape"),
        member("carla-ape", "Carlinha", userId = "carla"),
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        storedGroups: List<Group>,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        storedHidden: List<HiddenContact> = emptyList(),
    ) {
        groups = FakeGroupRepository(storedGroups)
        expenses = FakeExpenseRepository(storedExpenses)
        settlements = FakeSettlementRepository(storedSettlements)
        hidden = FakeHiddenContactRepository(
            now = { 100L },
            initialHidden = storedHidden,
        )
        useCase = ObserveContactBookUseCase(
            observeGroups = ObserveGroupsUseCase(groups),
            observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
            hiddenContacts = hidden,
        )
    }
}
