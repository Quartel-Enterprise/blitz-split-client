package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteExpenseUseCaseTest {
    private lateinit var repository: FakeExpenseRepository
    private lateinit var deletedBills: InMemoryDeletedBillRepository
    private lateinit var noticeBillGone: NoticeBillGoneUseCase
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

    @Test
    fun `GIVEN a delete in flight WHEN the open screen sees the bill go THEN no group is owed a notice`() = runTest {
        // Given
        prepareScenario(deleteGate = CompletableDeferred())
        backgroundScope.launch { useCase(BILL_ID) }
        runCurrent()

        // When
        noticeBillGone(
            groupId = GROUP_ID,
            expenseId = BILL_ID,
        )

        // Then
        assertTrue(deletedBills.isDeletedHere(BILL_ID))
        assertNull(deletedBills.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN a bill deleted from Editar conta WHEN Conta below sees it gone THEN no group is owed a notice`() =
        runTest {
            // Given
            prepareScenario()
            useCase(BILL_ID)

            // When
            noticeBillGone(
                groupId = GROUP_ID,
                expenseId = BILL_ID,
            )

            // Then
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the device fails to save WHEN deleting a bill THEN the bill is not taken as deleted here`() = runTest {
        // Given
        prepareScenario(deleteError = IllegalStateException("disk full"))

        // When
        useCase(BILL_ID)

        // Then
        assertFalse(deletedBills.isDeletedHere(BILL_ID))
    }

    @Test
    fun `GIVEN a delete here that failed WHEN a sync later removes the bill THEN its group is owed the notice`() =
        runTest {
            // Given
            prepareScenario(deleteError = IllegalStateException("disk full"))
            useCase(BILL_ID)

            // When
            noticeBillGone(
                groupId = GROUP_ID,
                expenseId = BILL_ID,
            )

            // Then
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    private val mercado = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 1_000L,
        splitMode = SplitMode.EQUAL,
        payers = emptyList(),
        shares = emptyList(),
        items = null,
        spentAt = 5_000L,
        isPending = false,
        updatedAt = 2_000L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        now: Long = 1_000L,
        stored: List<Expense> = listOf(mercado),
        deleteError: Throwable? = null,
        deleteGate: CompletableDeferred<Unit>? = null,
    ) {
        repository = FakeExpenseRepository(initialExpenses = stored).also {
            it.deleteError = deleteError
            it.deleteGate = deleteGate
        }
        deletedBills = InMemoryDeletedBillRepository()
        noticeBillGone = NoticeBillGoneUseCase(deletedBills)
        useCase = DeleteExpenseUseCase(
            repository = repository,
            deletedBills = deletedBills,
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val BILL_ID = "bill-1"
        const val GROUP_ID = "group-1"
    }
}
