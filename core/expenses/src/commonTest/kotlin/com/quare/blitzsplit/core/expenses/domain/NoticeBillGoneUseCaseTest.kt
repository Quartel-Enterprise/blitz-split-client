package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoticeBillGoneUseCaseTest {
    private val deletedBills = InMemoryDeletedBillRepository()
    private val useCase = NoticeBillGoneUseCase(deletedBills)

    @Test
    fun `GIVEN a bill this device did not delete WHEN it goes THEN its group is owed the notice`() = runTest {
        // Given
        val billId = BILL_ID

        // When
        useCase(
            groupId = GROUP_ID,
            expenseId = billId,
        )

        // Then
        assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN the bill was deleted on this device WHEN it goes THEN no group is owed a notice`() = runTest {
        // Given
        deletedBills.markDeletedHere(BILL_ID)

        // When
        useCase(
            groupId = GROUP_ID,
            expenseId = BILL_ID,
        )

        // Then
        assertNull(deletedBills.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN the delete on this device failed WHEN a sync removes the bill THEN its group is owed the notice`() =
        runTest {
            // Given
            deletedBills.markDeletedHere(BILL_ID)
            deletedBills.unmarkDeletedHere(BILL_ID)

            // When
            useCase(
                groupId = GROUP_ID,
                expenseId = BILL_ID,
            )

            // Then
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN another bill was deleted on this device WHEN this bill goes THEN its group is owed the notice`() =
        runTest {
            // Given
            deletedBills.markDeletedHere(OTHER_BILL_ID)

            // When
            useCase(
                groupId = GROUP_ID,
                expenseId = BILL_ID,
            )

            // Then
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN another group is owed a notice WHEN a bill of this group goes THEN only this group is owed it`() =
        runTest {
            // Given
            deletedBills.postNotice(OTHER_GROUP_ID)

            // When
            useCase(
                groupId = GROUP_ID,
                expenseId = BILL_ID,
            )

            // Then
            assertEquals(GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN another group is owed a notice WHEN a bill deleted here goes THEN the other notice keeps waiting`() =
        runTest {
            // Given
            deletedBills.postNotice(OTHER_GROUP_ID)
            deletedBills.markDeletedHere(BILL_ID)

            // When
            useCase(
                groupId = GROUP_ID,
                expenseId = BILL_ID,
            )

            // Then
            assertEquals(OTHER_GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    private companion object {
        const val BILL_ID = "bill-1"
        const val OTHER_BILL_ID = "bill-2"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}
