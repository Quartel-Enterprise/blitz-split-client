package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveExpensesUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: ObserveExpensesUseCase

    @Test
    fun `GIVEN bills of two groups WHEN observing one group THEN shows only its bills with the most recent first`() =
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
            )

            // When
            val expenses = useCase("group-1").first()

            // Then
            assertEquals(listOf("newer", "older"), expenses.map { it.id })
        }

    @Test
    fun `GIVEN an open group WHEN a bill is added to it THEN the group shows it`() = runTest {
        // Given
        prepareScenario()
        val expenses = useCase("group-1")

        // When
        repository.expenses.value = listOf(
            expense(
                id = "new",
                groupId = "group-1",
                spentAt = 1_000L,
            ),
        )

        // Then
        assertEquals(listOf("new"), expenses.first().map { it.id })
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
        spentAt = spentAt,
        isPending = false,
    )

    private fun prepareScenario(stored: List<Expense> = emptyList()) {
        repository = FakeExpenseRepository(initialExpenses = stored)
        useCase = ObserveExpensesUseCase(repository)
    }
}
