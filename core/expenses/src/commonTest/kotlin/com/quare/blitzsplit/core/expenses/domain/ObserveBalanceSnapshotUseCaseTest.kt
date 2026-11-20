package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BalanceSnapshot
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.OpenGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBalanceSnapshotUseCase
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveBalanceSnapshotUseCaseTest {
    private lateinit var groups: MutableStateFlow<List<Group>>
    private lateinit var money: MutableStateFlow<Map<String, GroupMoney>>
    private lateinit var useCase: ObserveBalanceSnapshotUseCase
    private val snapshots = mutableListOf<BalanceSnapshot>()

    @Test
    fun `GIVEN open groups WHEN observing THEN lists the most recently used group first`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group("recent", updatedAt = 5_000L), group("old", updatedAt = 1_000L)),
            storedMoney = mapOf(
                "recent" to moneyOf(anaOwesMe("recent", 1_000L, updatedAt = 5_000L)),
                "old" to moneyOf(anaOwesMe("old", 2_000L, updatedAt = 9_000L)),
            ),
        )

        // When
        runCurrent()

        // Then
        assertEquals(listOf("old", "recent"), snapshots.last().openGroups.map { it.groupId })
    }

    @Test
    fun `GIVEN a settled group WHEN a bill is added to it THEN emits a snapshot with the group open`() = runTest {
        // Given
        prepareScenario(storedGroups = listOf(group("ape", updatedAt = 1_000L)))
        runCurrent()

        // When
        money.value = mapOf("ape" to moneyOf(anaOwesMe("ape", 1_000L, updatedAt = 2_000L)))
        runCurrent()

        // Then
        assertEquals(2, snapshots.size)
        assertTrue(snapshots.first().isSettled)
        assertEquals(
            listOf(OpenGroupBalance("ape", "ape", GroupCurrency.BRL, 1_000L)),
            snapshots.last().openGroups,
        )
    }

    @Test
    fun `GIVEN an open group WHEN it is removed THEN emits a snapshot with no group`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group("ape", updatedAt = 1_000L)),
            storedMoney = mapOf("ape" to moneyOf(anaOwesMe("ape", 1_000L, updatedAt = 2_000L))),
        )
        runCurrent()

        // When
        groups.value = emptyList()
        runCurrent()

        // Then
        assertEquals(2, snapshots.size)
        assertEquals(
            BalanceSnapshot(
                overall = null,
                toReceive = null,
                toPay = null,
                openGroups = emptyList(),
                isSettled = true,
            ),
            snapshots.last(),
        )
    }

    @Test
    fun `GIVEN the snapshot shown WHEN a change leaves it the same THEN does not emit it again`() = runTest {
        // Given
        prepareScenario(
            storedGroups = listOf(group("ape", updatedAt = 1_000L)),
            storedMoney = mapOf("ape" to moneyOf(anaOwesMe("ape", 1_000L, updatedAt = 2_000L))),
        )
        runCurrent()

        // When
        groups.value = listOf(group("ape", updatedAt = 3_000L))
        runCurrent()

        // Then
        assertEquals(1, snapshots.size)
    }

    private val me = member("me", "Pierre", isMe = true)
    private val ana = member("ana", "Ana")

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = id,
        inviteCode = null,
        photoUrl = null,
    )

    private fun group(
        id: String,
        updatedAt: Long,
    ): Group = Group(
        id = id,
        name = id,
        type = GroupType.OTHER,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(me, ana),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
        reminder = GroupReminder.Default,
        reminderDismissedAt = 0L,
    )

    private fun moneyOf(vararg expenses: Expense): GroupMoney = GroupMoney(
        expenses = expenses.toList(),
        settlements = emptyList(),
        pendingDeletionCount = 0,
    )

    // Why: a bill this device's user paid all on Ana, saved at updatedAt, which the home's order reads.
    private fun anaOwesMe(
        groupId: String,
        amountMinor: Long,
        updatedAt: Long,
    ): Expense = Expense(
        id = "bill-$groupId",
        groupId = groupId,
        description = "Mercado",
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(me.id, amountMinor)),
        shares = listOf(
            ExpenseShare(
                memberId = ana.id,
                amountMinor = amountMinor,
                weight = amountMinor,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 0L,
        isPending = false,
        updatedAt = updatedAt,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedGroups: List<Group>,
        storedMoney: Map<String, GroupMoney> = emptyMap(),
    ) {
        groups = MutableStateFlow(storedGroups)
        money = MutableStateFlow(storedMoney)
        useCase = ObserveBalanceSnapshotUseCase(
            observeGroups = { groups },
            observeGroupsMoney = { money },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect(snapshots::add) }
    }
}
