package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveDeletedBillNoticeUseCaseTest {
    private val deletedBills = InMemoryDeletedBillRepository()
    private val useCase = ObserveDeletedBillNoticeUseCase(deletedBills)
    private val emissions = mutableListOf<Boolean>()

    @Test
    fun `GIVEN no group is owed a notice WHEN the group observes it THEN shows none`() = runTest {
        // Given
        val groupId = GROUP_ID

        // When
        val owed = useCase(groupId).first()

        // Then
        assertFalse(owed)
    }

    @Test
    fun `GIVEN the group is owed the notice WHEN the group observes it THEN shows it`() = runTest {
        // Given
        deletedBills.postNotice(GROUP_ID)

        // When
        val owed = useCase(GROUP_ID).first()

        // Then
        assertTrue(owed)
    }

    @Test
    fun `GIVEN another group is owed the notice WHEN this group observes it THEN shows none`() = runTest {
        // Given
        deletedBills.postNotice(OTHER_GROUP_ID)

        // When
        val owed = useCase(GROUP_ID).first()

        // Then
        assertFalse(owed)
    }

    @Test
    fun `GIVEN the group is observing WHEN a bill of the group is deleted elsewhere THEN shows the notice`() = runTest {
        // Given
        backgroundScope.launch { useCase(GROUP_ID).collect(emissions::add) }
        runCurrent()

        // When
        deletedBills.postNotice(GROUP_ID)
        runCurrent()

        // Then
        assertEquals(listOf(false, true), emissions)
    }

    @Test
    fun `GIVEN the group shows the notice WHEN the group consumes it THEN stops showing it`() = runTest {
        // Given
        deletedBills.postNotice(GROUP_ID)
        backgroundScope.launch { useCase(GROUP_ID).collect(emissions::add) }
        runCurrent()

        // When
        deletedBills.consumeNotice(GROUP_ID)
        runCurrent()

        // Then
        assertEquals(listOf(true, false), emissions)
    }

    @Test
    fun `GIVEN the group shows the notice WHEN a newer notice goes to another group THEN stops showing it`() = runTest {
        // Given
        deletedBills.postNotice(GROUP_ID)
        backgroundScope.launch { useCase(GROUP_ID).collect(emissions::add) }
        runCurrent()

        // When
        deletedBills.postNotice(OTHER_GROUP_ID)
        runCurrent()

        // Then
        assertEquals(listOf(true, false), emissions)
    }

    @Test
    fun `GIVEN the group is observing WHEN the notice moves between other groups THEN shows none only once`() =
        runTest {
            // Given
            backgroundScope.launch { useCase(GROUP_ID).collect(emissions::add) }
            runCurrent()

            // When
            deletedBills.postNotice(OTHER_GROUP_ID)
            runCurrent()
            deletedBills.postNotice(THIRD_GROUP_ID)
            runCurrent()
            deletedBills.consumeNotice(THIRD_GROUP_ID)
            runCurrent()

            // Then
            assertEquals(listOf(false), emissions)
        }

    private companion object {
        const val GROUP_ID = "group-1"
        const val OTHER_GROUP_ID = "group-2"
        const val THIRD_GROUP_ID = "group-3"
    }
}
