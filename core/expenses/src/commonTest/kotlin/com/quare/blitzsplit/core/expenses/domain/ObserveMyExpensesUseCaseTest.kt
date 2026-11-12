package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMyExpensesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Why: Home's "Latest expenses" and "All expenses", the bills this device's user is in across groups.
class ObserveMyExpensesUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: ObserveMyExpensesUseCase

    @Test
    fun `GIVEN my bills in two groups WHEN observing a page of them THEN asks for that many and gives them`() =
        runTest {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(
                        id = "older",
                        groupId = "group-1",
                        spentAt = 1_000L,
                    ),
                    expense(
                        id = "newer",
                        groupId = "group-2",
                        spentAt = 3_000L,
                    ),
                    expense(
                        id = "middle",
                        groupId = "group-1",
                        spentAt = 2_000L,
                    ),
                ),
            )

            // When
            val expenses = useCase(limit = 2).first()

            // Then
            assertEquals(listOf(2), repository.myExpenseLimits)
            assertEquals(listOf("newer", "middle"), expenses.map { it.id })
        }

    @Test
    fun `GIVEN a bill I am not in WHEN observing my bills THEN leaves it out`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                expense(
                    id = "mine",
                    groupId = "group-1",
                    spentAt = 1_000L,
                ),
                expense(
                    id = "theirs",
                    groupId = "group-1",
                    spentAt = 2_000L,
                    payerId = "ana",
                ),
            ),
            myMemberIds = setOf("pierre"),
        )

        // When
        val expenses = useCase(limit = 10).first()

        // Then
        assertEquals(listOf("mine"), expenses.map { it.id })
    }

    private fun expense(
        id: String,
        groupId: String,
        spentAt: Long,
        payerId: String = "pierre",
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        currency = GroupCurrency.BRL,
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(payerId, 1_000L)),
        shares = emptyList(),
        items = null,
        categoryId = null,
        spentAt = spentAt,
        isPending = false,
        updatedAt = 1L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        stored: List<Expense>,
        myMemberIds: Set<String>? = null,
    ) {
        repository = FakeExpenseRepository(initialExpenses = stored).also { it.myMemberIds.value = myMemberIds }
        useCase = ObserveMyExpensesUseCase(repository)
    }
}
