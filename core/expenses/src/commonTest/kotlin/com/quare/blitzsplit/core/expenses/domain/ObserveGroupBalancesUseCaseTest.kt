package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveGroupBalancesUseCaseTest {
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var groups: FakeGroupRepository
    private lateinit var useCase: ObserveGroupBalancesUseCase
    private val seen = mutableListOf<GroupBalances>()

    @Test
    fun `GIVEN bills and payments of the group and of another WHEN observing the group THEN counts only its own`() =
        runTest {
            // Given
            val otherGroupBill = anaOwesPierre(
                id = "other-bill",
                groupId = OTHER_GROUP_ID,
                amountMinor = 900L,
            )
            val otherGroupPayment = anaPaysPierre(
                id = "other-payment",
                groupId = OTHER_GROUP_ID,
                amountMinor = 900L,
            )
            prepareScenario(
                storedExpenses = listOf(
                    anaOwesPierre(
                        id = "dinner",
                        amountMinor = 100L,
                    ),
                    otherGroupBill,
                ),
                storedSettlements = listOf(
                    anaPaysPierre(
                        id = "pix",
                        amountMinor = 40L,
                    ),
                    otherGroupPayment,
                ),
            )

            // When
            val balances = useCase(GROUP_ID).first()

            // Then
            assertEquals(
                GroupBalances(
                    debts = listOf(Debt(ANA, PIERRE, 60L)),
                    hasBills = true,
                    unsimplifiedDebts = null,
                ),
                balances,
            )
        }

    @Test
    fun `GIVEN an open group with no bills WHEN observing it THEN nobody owes anything`() = runTest {
        // Given
        prepareScenario(storedExpenses = emptyList())

        // When
        val balances = useCase(GROUP_ID).first()

        // Then
        assertFalse(balances.hasBills)
        assertTrue(balances.debts.isEmpty())
    }

    @Test
    fun `GIVEN open balances WHEN a bill is added to the group THEN the balances count it`() = runTest {
        // Given
        prepareScenario()
        watchBalances()

        // When
        expenses.expenses.update {
            it + anaOwesPierre(
                id = "dinner",
                amountMinor = 100L,
            )
        }

        // Then
        assertEquals(listOf(Debt(ANA, PIERRE, 100L)), seen.last().debts)
        assertEquals(2, seen.size)
    }

    @Test
    fun `GIVEN open balances with a debt WHEN a payment is recorded THEN the balances count it`() = runTest {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                anaOwesPierre(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )
        watchBalances()

        // When
        settlements.settlements.update {
            it + anaPaysPierre(
                id = "pix",
                amountMinor = 100L,
            )
        }

        // Then
        assertTrue(seen.last().isEveryoneSettled)
        assertEquals(2, seen.size)
    }

    @Test
    fun `GIVEN open balances with a payment WHEN the payment is undone THEN the debt comes back`() = runTest {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                anaOwesPierre(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            storedSettlements = listOf(
                anaPaysPierre(
                    id = "pix",
                    amountMinor = 100L,
                ),
            ),
        )
        watchBalances()

        // When
        settlements.settlements.value = emptyList()

        // Then
        assertEquals(listOf(Debt(ANA, PIERRE, 100L)), seen.last().debts)
    }

    @Test
    fun `GIVEN open balances WHEN a bill of another group is added THEN the balances do not change again`() = runTest {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                anaOwesPierre(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )
        watchBalances()

        // When
        expenses.expenses.update {
            it + anaOwesPierre(
                id = "other-bill",
                groupId = OTHER_GROUP_ID,
                amountMinor = 900L,
            )
        }

        // Then
        assertEquals(1, seen.size)
    }

    @Test
    fun `GIVEN a chain of debts in a group with simplifying on WHEN observing it THEN shows the simplified pairs`() =
        runTest {
            // Given
            prepareScenario(
                storedExpenses = chain,
                storedGroups = listOf(group(simplifiesDebts = true)),
            )

            // When
            val balances = useCase(GROUP_ID).first()

            // Then
            assertEquals(listOf(Debt(BRUNO, PIERRE, 100L)), balances.debts)
            assertTrue(balances.isSimplified)
            assertEquals(2, balances.unsimplifiedCount)
        }

    @Test
    fun `GIVEN a chain of debts and simplifying off WHEN observing the group THEN shows the pairs the bills make`() =
        runTest {
            // Given
            prepareScenario(
                storedExpenses = chain,
                storedGroups = listOf(group(simplifiesDebts = false)),
            )

            // When
            val balances = useCase(GROUP_ID).first()

            // Then
            assertEquals(listOf(Debt(ANA, PIERRE, 100L), Debt(BRUNO, ANA, 100L)), balances.debts)
            assertFalse(balances.isSimplified)
        }

    @Test
    fun `GIVEN the group is being observed WHEN simplifying is turned on THEN the balances follow it`() = runTest {
        // Given
        prepareScenario(
            storedExpenses = chain,
            storedGroups = listOf(group(simplifiesDebts = false)),
        )
        watchBalances()

        // When
        groups.groups.value = listOf(group(simplifiesDebts = true))

        // Then
        assertEquals(listOf(Debt(BRUNO, PIERRE, 100L)), seen.last().debts)
        assertEquals(2, seen.size)
    }

    @Test
    fun `GIVEN a simplified group being observed WHEN simplifying is turned off THEN the raw pairs come back`() =
        runTest {
            // Given
            prepareScenario(
                storedExpenses = chain,
                storedGroups = listOf(group(simplifiesDebts = true)),
            )
            watchBalances()

            // When
            groups.groups.value = listOf(group(simplifiesDebts = false))

            // Then
            assertEquals(listOf(Debt(ANA, PIERRE, 100L), Debt(BRUNO, ANA, 100L)), seen.last().debts)
            assertFalse(seen.last().isSimplified)
        }

    @Test
    fun `GIVEN a simplified group being observed WHEN the group leaves the device THEN shows the raw pairs`() =
        runTest {
            // Given
            prepareScenario(
                storedExpenses = chain,
                storedGroups = listOf(group(simplifiesDebts = true)),
            )
            watchBalances()

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(listOf(Debt(ANA, PIERRE, 100L), Debt(BRUNO, ANA, 100L)), seen.last().debts)
            assertFalse(seen.last().isSimplified)
        }

    @Test
    fun `GIVEN a group being observed WHEN another group changes THEN the balances do not change again`() = runTest {
        // Given
        prepareScenario(
            storedExpenses = chain,
            storedGroups = listOf(
                group(simplifiesDebts = true),
                group(
                    simplifiesDebts = false,
                    id = OTHER_GROUP_ID,
                ),
            ),
        )
        watchBalances()

        // When
        groups.groups.value = listOf(
            group(simplifiesDebts = true),
            group(
                simplifiesDebts = true,
                id = OTHER_GROUP_ID,
            ),
        )

        // Then
        assertEquals(1, seen.size)
    }

    // Why: Bruno owes Ana and Ana owes Pierre the same, which simplifying turns into Bruno paying Pierre.
    private val chain = listOf(
        owes(BRUNO, ANA, 100L, id = "uber"),
        owes(ANA, PIERRE, 100L, id = "pousada"),
    )

    private fun group(
        simplifiesDebts: Boolean,
        id: String = GROUP_ID,
    ): Group = Group(
        id = id,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(PIERRE, ANA, BRUNO).map { memberId ->
            GroupMember(
                id = memberId,
                name = memberId,
                avatarColor = AvatarColor.BLUE,
                isMe = memberId == PIERRE,
                hasJoined = true,
                userId = memberId,
                inviteCode = null,
                photoUrl = null,
            )
        },
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = simplifiesDebts,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun owes(
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        id: String,
    ): Expense = anaOwesPierre(
        id = id,
        amountMinor = amountMinor,
    ).copy(
        payers = listOf(
            ExpensePayer(
                memberId = toMemberId,
                amountMinor = amountMinor,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = fromMemberId,
                amountMinor = amountMinor,
                weight = amountMinor,
            ),
        ),
    )

    private fun anaOwesPierre(
        id: String,
        amountMinor: Long,
        groupId: String = GROUP_ID,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Jantar",
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(
            ExpensePayer(
                memberId = PIERRE,
                amountMinor = amountMinor,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = ANA,
                amountMinor = amountMinor,
                weight = amountMinor,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun anaPaysPierre(
        id: String,
        amountMinor: Long,
        groupId: String = GROUP_ID,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = ANA,
        toMemberId = PIERRE,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = true,
        updatedAt = 0L,
    )

    private fun TestScope.watchBalances() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase(GROUP_ID).toList(seen) }
    }

    private fun prepareScenario(
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        storedGroups: List<Group> = emptyList(),
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        useCase = ObserveGroupBalancesUseCase(
            expenseRepository = expenses,
            settlementRepository = settlements,
            groupRepository = groups,
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BRUNO = "bruno"
    }
}
