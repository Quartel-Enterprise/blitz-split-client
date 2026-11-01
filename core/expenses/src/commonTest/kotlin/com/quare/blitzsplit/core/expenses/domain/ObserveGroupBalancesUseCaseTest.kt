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
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        useCase = ObserveGroupBalancesUseCase(
            expenseRepository = expenses,
            settlementRepository = settlements,
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
        const val PIERRE = "pierre"
        const val ANA = "ana"
    }
}
