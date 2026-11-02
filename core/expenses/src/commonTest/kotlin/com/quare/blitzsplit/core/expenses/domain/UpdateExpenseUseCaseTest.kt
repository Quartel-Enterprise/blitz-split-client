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
            payers = listOf(
                ExpensePayer(
                    memberId = "ana",
                    amountMinor = 1_500L,
                ),
            ),
            splitMode = SplitMode.AMOUNT,
            shares = editedShares,
            items = null,
            categoryId = null,
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
    fun `GIVEN several payers WHEN saving the edit THEN stores what each one paid`() = runTest {
        // Given
        prepareScenario()
        val payers = listOf(
            ExpensePayer(
                memberId = "pierre",
                amountMinor = 600L,
            ),
            ExpensePayer(
                memberId = "ana",
                amountMinor = 400L,
            ),
        )

        // When
        updateExpense(payers = payers)

        // Then
        assertEquals(payers, repository.updatedExpenses.single().payers)
    }

    @Test
    fun `GIVEN a bill paid by two WHEN the edit leaves one payer THEN the bill keeps only that payer`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                mercado.copy(
                    payers = listOf(
                        ExpensePayer(
                            memberId = "pierre",
                            amountMinor = 600L,
                        ),
                        ExpensePayer(
                            memberId = "ana",
                            amountMinor = 400L,
                        ),
                    ),
                ),
            ),
        )

        // When
        updateExpense()

        // Then
        assertEquals(
            listOf(
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 1_000L,
                ),
            ),
            repository.expenses.value
                .single()
                .payers,
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

    @Test
    fun `GIVEN payers adding up to more than the bill WHEN saving the edit THEN refuses it and keeps the bill`() =
        runTest {
            // Given
            prepareScenario()
            val payers = listOf(
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 600L,
                ),
                ExpensePayer(
                    memberId = "ana",
                    amountMinor = 500L,
                ),
            )

            // When
            val result = updateExpense(
                description = "Feira",
                payers = payers,
            )

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertTrue(repository.updatedExpenses.isEmpty())
            assertTrue(repository.updatedAts.isEmpty())
            assertEquals(listOf(mercado), repository.expenses.value)
        }

    @Test
    fun `GIVEN the same member twice among the payers WHEN saving the edit THEN refuses it and keeps the bill`() =
        runTest {
            // Given
            prepareScenario()
            val payers = listOf(
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 500L,
                ),
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 500L,
                ),
            )

            // When
            val result = updateExpense(payers = payers)

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertTrue(repository.updatedExpenses.isEmpty())
            assertEquals(listOf(mercado), repository.expenses.value)
        }

    @Test
    fun `GIVEN no share WHEN saving the edit THEN refuses it and keeps the bill`() = runTest {
        // Given
        prepareScenario()
        val shares = emptyList<ExpenseShare>()

        // When
        val result = updateExpense(shares = shares)

        // Then
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(repository.updatedExpenses.isEmpty())
        assertEquals(listOf(mercado), repository.expenses.value)
    }

    @Test
    fun `GIVEN a negative share adding up to the bill WHEN saving the edit THEN refuses it and keeps the bill`() =
        runTest {
            // Given
            prepareScenario()
            val shares = listOf(
                ExpenseShare(
                    memberId = "pierre",
                    amountMinor = 1_200L,
                    weight = 1L,
                ),
                ExpenseShare(
                    memberId = "ana",
                    amountMinor = -200L,
                    weight = 1L,
                ),
            )

            // When
            val result = updateExpense(shares = shares)

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
            assertTrue(repository.updatedExpenses.isEmpty())
            assertEquals(listOf(mercado), repository.expenses.value)
        }

    @Test
    fun `GIVEN a wrong split on a bill deleted meanwhile WHEN saving the edit THEN refuses the split first`() =
        runTest {
            // Given
            prepareScenario(stored = emptyList())
            val payers = listOf(
                ExpensePayer(
                    memberId = "pierre",
                    amountMinor = 900L,
                ),
            )

            // When
            val result = updateExpense(payers = payers)

            // Then
            assertIs<IllegalArgumentException>(result.exceptionOrNull())
        }

    private suspend fun updateExpense(
        description: String = "Mercado",
        amountMinor: Long = 1_000L,
        payers: List<ExpensePayer> = listOf(
            ExpensePayer(
                memberId = "pierre",
                amountMinor = amountMinor,
            ),
        ),
        shares: List<ExpenseShare> = mercado.shares,
    ): Result<Unit> = useCase(
        expenseId = BILL_ID,
        groupId = GROUP_ID,
        description = description,
        amountMinor = amountMinor,
        payers = payers,
        splitMode = SplitMode.EQUAL,
        shares = shares,
        items = null,
        categoryId = null,
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
        items = null,
        categoryId = null,
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
