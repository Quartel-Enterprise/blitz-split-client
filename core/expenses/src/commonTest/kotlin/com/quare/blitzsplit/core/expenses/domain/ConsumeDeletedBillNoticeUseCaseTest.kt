package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConsumeDeletedBillNoticeUseCaseTest {
    private val deletedBills = InMemoryDeletedBillRepository()
    private val useCase = ConsumeDeletedBillNoticeUseCase(deletedBills)

    @Test
    fun `GIVEN the group is owed the notice WHEN the group consumes it THEN no group is owed one`() = runTest {
        // Given
        deletedBills.postNotice(GROUP_ID)

        // When
        useCase(GROUP_ID)

        // Then
        assertNull(deletedBills.observeNoticeGroupId().first())
    }

    @Test
    fun `GIVEN another group is owed the notice WHEN this group consumes its own THEN the notice keeps waiting`() =
        runTest {
            // Given
            deletedBills.postNotice(OTHER_GROUP_ID)

            // When
            useCase(GROUP_ID)

            // Then
            assertEquals(OTHER_GROUP_ID, deletedBills.observeNoticeGroupId().first())
        }

    private companion object {
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
    }
}
