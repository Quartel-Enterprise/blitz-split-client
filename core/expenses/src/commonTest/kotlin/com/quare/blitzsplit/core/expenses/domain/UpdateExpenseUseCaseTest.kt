package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateExpenseUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: UpdateExpenseUseCase

    @Test
    fun `GIVEN a bill WHEN saving the edit THEN stores what was typed pending at the current time`() = runTest {
        // Given
        prepareScenario(now = 9_000L)

        // When
        val result = useCase(
            expenseId = BILL_ID,
            groupId = GROUP_ID,
            description = "Feira",
            amountMinor = 1_500L,
            payerId = "ana",
            splitMode = SplitMode.AMOUNT,
            shares = editedShares,
            spentAt = 6_000L,
        )

        // Then
        assertTrue(result.isSuccess)
        val saved = repository.updatedExpenses.single()
        assertEquals(BILL_ID, saved.id)
        assertEquals(GROUP_ID, saved.groupId)
        assertEquals("Feira", saved.description)
        assertEquals(1_500L, saved.amountMinor)
        assertEquals(SplitMode.AMOUNT, saved.splitMode)
        assertEquals(editedShares, saved.shares)
        assertEquals(6_000L, saved.spentAt)
        assertTrue(saved.isPending)
        assertEquals(9_000L, saved.updatedAt)
        assertEquals(listOf(9_000L), repository.updatedAts)
    }

    @Test
    fun `GIVEN a bill another member saved WHEN saving the edit here THEN sends no editor since the server sets it`() =
        runTest {
            // Given
            prepareScenario()

            // When
            updateExpense()

            // Then
            assertNull(repository.updatedExpenses.single().updatedByMemberId)
        }

    @Test
    fun `GIVEN a new total and payer WHEN saving the edit THEN that payer alone paid the whole new total`() = runTest {
        // Given
        prepareScenario()

        // When
        updateExpense(
            amountMinor = 6_240L,
            payerId = "ana",
        )

        // Then
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = "ana",
                    amountMinor = 6_240L,
                ),
            ),
            repository.updatedExpenses.single().payers,
        )
    }

    @Test
    fun `GIVEN a description between spaces WHEN saving the edit THEN stores it trimmed`() = runTest {
        // Given
        prepareScenario()

        // When
        updateExpense(description = "  Feira  ")

        // Then
        assertEquals("Feira", repository.updatedExpenses.single().description)
    }

    @Test
    fun `GIVEN a long description with a space at the 100th character WHEN saving the edit THEN stores it capped`() =
        runTest {
            // Given
            prepareScenario()

            // When
            updateExpense(description = "a".repeat(99) + " bcd")

            // Then
            assertEquals("a".repeat(99), repository.updatedExpenses.single().description)
        }

    @Test
    fun `GIVEN the bill was deleted meanwhile WHEN saving the edit THEN fails and nothing is stored`() = runTest {
        // Given
        prepareScenario(stored = emptyList())

        // When
        val result = updateExpense()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.updatedExpenses.isEmpty())
        assertTrue(repository.expenses.value.isEmpty())
    }

    @Test
    fun `GIVEN the device fails to save WHEN saving the edit THEN fails and the bill stays as it was`() = runTest {
        // Given
        prepareScenario(updateError = IllegalStateException("disk full"))

        // When
        val result = updateExpense(description = "Feira")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(listOf(mercado), repository.expenses.value)
    }

    private suspend fun updateExpense(
        description: String = "Mercado",
        amountMinor: Long = 1_000L,
        payerId: String = "pierre",
    ): Result<Unit> = useCase(
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        description = description,
        amountMinor = amountMinor,
        payerId = payerId,
        splitMode = SplitMode.EQUAL,
        shares = mercado.shares,
        spentAt = 5_000L,
    )

    private val editedShares = listOf(
        ExpenseShare(
            memberId = "pierre",
            amountMinor = 1_000L,
            weight = 1_000L,
        ),
        ExpenseShare(
            memberId = "ana",
            amountMinor = 500L,
            weight = 500L,
        ),
    )

    private val mercado = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(
            ExpensePayer(
                memberId = "pierre",
                amountMinor = 1_000L,
            ),
        ),
        shares = listOf(
            ExpenseShare(
                memberId = "pierre",
                amountMinor = 500L,
                weight = 1L,
            ),
            ExpenseShare(
                memberId = "ana",
                amountMinor = 500L,
                weight = 1L,
            ),
        ),
        spentAt = 5_000L,
        isPending = false,
        updatedAt = 2_000L,
        updatedByMemberId = "ana",
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        stored: List<Expense> = listOf(mercado),
        updateError: Throwable? = null,
    ) {
        repository = FakeExpenseRepository(initialExpenses = stored).also { it.updateError = updateError }
        useCase = UpdateExpenseUseCase(
            repository = repository,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val BILL_ID = "bill-1"
        const val GROUP_ID = "group-1"
    }
}
