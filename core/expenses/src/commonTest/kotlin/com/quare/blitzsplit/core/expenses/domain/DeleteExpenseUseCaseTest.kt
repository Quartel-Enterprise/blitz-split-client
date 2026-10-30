package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteExpenseUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: DeleteExpenseUseCase

    @Test
    fun `GIVEN a bill WHEN deleting it THEN deletes it at the current time and the bill is gone`() = runTest {
        // Given
        prepareScenario(now = 9_000L)

        // When
        val result = useCase(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(BILL_ID), repository.deletedIds)
        assertEquals(listOf(9_000L), repository.deletedAts)
        assertNull(repository.observeExpense(BILL_ID).first())
    }

    @Test
    fun `GIVEN a bill already deleted WHEN deleting it THEN fails`() = runTest {
        // Given
        prepareScenario(stored = emptyList())

        // When
        val result = useCase(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN deleting a bill THEN fails and the bill stays`() = runTest {
        // Given
        prepareScenario(deleteError = IllegalStateException("disk full"))

        // When
        val result = useCase(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(BILL_ID), repository.expenses.value.map { it.id })
    }

    private val mercado = Expense(
        id = BILL_ID,
        groupId = "group-1",
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = emptyList(),
        shares = emptyList(),
        spentAt = 5_000L,
        isPending = false,
        updatedAt = 2_000L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        stored: List<Expense> = listOf(mercado),
        deleteError: Throwable? = null,
    ) {
        repository = FakeExpenseRepository(initialExpenses = stored).also { it.deleteError = deleteError }
        useCase = DeleteExpenseUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
