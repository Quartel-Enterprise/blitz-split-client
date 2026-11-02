package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveExpenseUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var useCase: ObserveExpenseUseCase

    @Test
    fun `GIVEN a bill another member saved WHEN observing it THEN shows it with that member and synced`() = runTest {
        // Given
        prepareScenario(stored = listOf(mercado, otherBill))

        // When
        val expense = useCase(BILL_ID).first()

        // Then
        assertEquals(mercado, expense)
        assertEquals("ana", expense?.updatedByMemberId)
        assertEquals(false, expense?.isPending)
    }

    @Test
    fun `GIVEN no such bill on the device WHEN observing it THEN shows no bill`() = runTest {
        // Given
        prepareScenario(stored = listOf(otherBill))

        // When
        val expense = useCase(BILL_ID).first()

        // Then
        assertNull(expense)
    }

    @Test
    fun `GIVEN an open bill WHEN it is deleted THEN shows no bill so the screen can close`() = runTest {
        // Given
        prepareScenario(stored = listOf(mercado))
        val emissions = mutableListOf<Expense?>()
        backgroundScope.launch { useCase(BILL_ID).collect(emissions::add) }
        runCurrent()

        // When
        repository.deleteExpense(
            id = BILL_ID,
            deletedAt = 9_000L,
        )
        runCurrent()

        // Then
        assertEquals(listOf(BILL_ID, null), emissions.map { it?.id })
    }

    @Test
    fun `GIVEN an open bill WHEN it is edited offline THEN shows the new version pending`() = runTest {
        // Given
        prepareScenario(stored = listOf(mercado))
        val emissions = mutableListOf<Expense?>()
        backgroundScope.launch { useCase(BILL_ID).collect(emissions::add) }
        runCurrent()

        // When
        repository.updateExpense(
            expense = mercado.copy(description = "Feira"),
            updatedAt = 9_000L,
        )
        runCurrent()

        // Then
        val latest = emissions.last()
        assertEquals("Feira", latest?.description)
        assertEquals(true, latest?.isPending)
    }

    private val mercado = Expense(
        id = BILL_ID,
        groupId = "group-1",
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

    private val otherBill = mercado.copy(
        id = "bill-2",
        description = "Pizza",
    )

    private fun prepareScenario(stored: List<Expense>) {
        repository = FakeExpenseRepository(initialExpenses = stored)
        useCase = ObserveExpenseUseCase(repository)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
