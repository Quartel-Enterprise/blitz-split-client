package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveGroupsMoneyUseCaseTest {
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var useCase: ObserveGroupsMoneyUseCase

    @Test
    fun `GIVEN bills and payments of two groups WHEN observing THEN gives each group its own most recent first`() =
        runTest {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "older",
                        groupId = "group-1",
                        spentAt = 1_000L,
                    ),
                    expense(
                        id = "other-group",
                        groupId = "group-2",
                        spentAt = 2_000L,
                    ),
                    expense(
                        id = "newer",
                        groupId = "group-1",
                        spentAt = 3_000L,
                    ),
                ),
                storedSettlements = listOf(
                    payment(
                        id = "paid",
                        groupId = "group-2",
                    ),
                ),
            )

            // When
            val money = useCase().first()

            // Then
            assertEquals(setOf("group-1", "group-2"), money.keys)
            assertEquals(listOf("newer", "older"), money.getValue("group-1").expenses.map { it.id })
            assertTrue(money.getValue("group-1").settlements.isEmpty())
            assertEquals(listOf("other-group"), money.getValue("group-2").expenses.map { it.id })
            assertEquals(listOf("paid"), money.getValue("group-2").settlements.map { it.id })
        }

    @Test
    fun `GIVEN a group with only a payment WHEN observing THEN gives it the payment and no bill`() = runTest {
        // Given
        prepareScenario(storedSettlements = listOf(payment(id = "paid")))

        // When
        val money = useCase().first()

        // Then
        assertEquals(
            mapOf(
                "group-1" to GroupMoney(
                    expenses = emptyList(),
                    settlements = listOf(payment(id = "paid")),
                    pendingDeletionCount = 0,
                ),
            ),
            money,
        )
    }

    @Test
    fun `GIVEN a bill deleted and a payment undone here WHEN observing before the sync THEN counts both as waiting`() =
        runTest {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "dinner",
                        groupId = "group-1",
                        spentAt = 1_000L,
                    ),
                    expense(
                        id = "taxi",
                        groupId = "group-2",
                        spentAt = 1_000L,
                    ),
                ),
                storedSettlements = listOf(payment(id = "paid")),
            )
            expenses.deleteExpense("dinner", deletedAt = 9_000L)
            expenses.deleteExpense("taxi", deletedAt = 9_000L)
            settlements.deleteSettlements(listOf("paid"), deletedAt = 9_000L)

            // When
            val money = useCase().first()

            // Then
            assertEquals(
                mapOf(
                    "group-1" to GroupMoney(
                        expenses = emptyList(),
                        settlements = emptyList(),
                        pendingDeletionCount = 2,
                    ),
                    "group-2" to GroupMoney(
                        expenses = emptyList(),
                        settlements = emptyList(),
                        pendingDeletionCount = 1,
                    ),
                ),
                money,
            )
        }

    @Test
    fun `GIVEN nothing spent WHEN a bill is added THEN its group shows it`() = runTest {
        // Given
        prepareScenario()
        val money = useCase()

        // When
        expenses.expenses.value = listOf(
            expense(
                id = "new",
                groupId = "group-1",
                spentAt = 1_000L,
            ),
        )

        // Then
        assertEquals(
            listOf("new"),
            money
                .first()
                .getValue("group-1")
                .expenses
                .map { it.id },
        )
    }

    private fun expense(
        id: String,
        groupId: String,
        spentAt: Long,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = emptyList(),
        shares = emptyList(),
        items = null,
        spentAt = spentAt,
        isPending = false,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun payment(
        id: String,
        groupId: String = "group-1",
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = "ana",
        toMemberId = "bruno",
        amountMinor = 5_000L,
        method = SettlementMethod.PIX,
        paidAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
    )

    private fun prepareScenario(
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
    ) {
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        useCase = ObserveGroupsMoneyUseCase(
            expenses = expenses,
            settlements = settlements,
        )
    }
}
