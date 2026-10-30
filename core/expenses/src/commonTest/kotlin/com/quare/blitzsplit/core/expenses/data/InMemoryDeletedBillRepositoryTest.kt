package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryDeletedBillRepositoryTest {
    private val repository = InMemoryDeletedBillRepository()

    @Test
    fun `GIVEN a bill this device never deleted WHEN checking it THEN it is not deleted here`() {
        // Given
        val billId = BILL_ID

        // When
        val deletedHere = repository.isDeletedHere(billId)

        // Then
        assertFalse(deletedHere)
    }

    @Test
    fun `GIVEN a bill marked as deleted here WHEN checking it THEN it is deleted here`() {
        // Given
        repository.markDeletedHere(BILL_ID)

        // When
        val deletedHere = repository.isDeletedHere(BILL_ID)

        // Then
        assertTrue(deletedHere)
    }

    @Test
    fun `GIVEN a bill whose delete failed WHEN checking it THEN it is no longer deleted here`() {
        // Given
        repository.markDeletedHere(BILL_ID)
        repository.unmarkDeletedHere(BILL_ID)

        // When
        val deletedHere = repository.isDeletedHere(BILL_ID)

        // Then
        assertFalse(deletedHere)
    }

    @Test
    fun `GIVEN another bill deleted here WHEN checking this bill THEN it is not deleted here`() {
        // Given
        repository.markDeletedHere(OTHER_BILL_ID)

        // When
        val deletedHere = repository.isDeletedHere(BILL_ID)

        // Then
        assertFalse(deletedHere)
    }

    @Test
    fun `GIVEN two bills deleted here WHEN the delete of one fails THEN the other is still deleted here`() {
        // Given
        repository.markDeletedHere(BILL_ID)
        repository.markDeletedHere(OTHER_BILL_ID)

        // When
        repository.unmarkDeletedHere(OTHER_BILL_ID)

        // Then
        assertTrue(repository.isDeletedHere(BILL_ID))
        assertFalse(repository.isDeletedHere(OTHER_BILL_ID))
    }

    @Test
    fun `GIVEN no bill deleted elsewhere WHEN observing the notice THEN no group is owed one`() = runTest {
        // Given
        val notices = repository.observeNoticeGroupId()

        // When
        val groupId = notices.first()

        // Then
        assertNull(groupId)
    }

    @Test
    fun `GIVEN no notice WHEN posting one for a group THEN that group is owed the notice`() = runTest {
        // Given
        val groupId = GROUP_ID

        // When
        repository.postNotice(groupId)

        // Then
        assertEquals(GROUP_ID, repository.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN a notice for a group WHEN posting one for another group THEN only the newer group is owed it`() =
        runTest {
            // Given
            repository.postNotice(GROUP_ID)

            // When
            repository.postNotice(OTHER_GROUP_ID)

            // Then
            assertEquals(OTHER_GROUP_ID, repository.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN a notice for a group WHEN that group consumes it THEN no group is owed one`() = runTest {
        // Given
        repository.postNotice(GROUP_ID)

        // When
        repository.consumeNotice(GROUP_ID)

        // Then
        assertNull(repository.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN a notice for a group WHEN another group consumes its own THEN the notice keeps waiting`() = runTest {
        // Given
        repository.postNotice(GROUP_ID)

        // When
        repository.consumeNotice(OTHER_GROUP_ID)

        // Then
        assertEquals(GROUP_ID, repository.observeNoticeGroupId().first())
    }

    private companion object {
        const val BILL_ID = "bill-1"
        const val OTHER_BILL_ID = "bill-2"
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}
