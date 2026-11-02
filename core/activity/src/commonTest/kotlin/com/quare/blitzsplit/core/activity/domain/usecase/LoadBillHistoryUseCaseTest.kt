package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.usecase.impl.LoadBillHistoryUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoadBillHistoryUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: LoadBillHistoryUseCase

    @Test
    fun `GIVEN a bill WHEN loading its history THEN loads the server history of that bill`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase(BILL_ID)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(BILL_ID), activity.loadedHistoryIds)
    }

    @Test
    fun `GIVEN no connection WHEN loading a bill history THEN returns the failure`() = runTest {
        // Given
        prepareScenario()
        activity.loadHistoryError = IllegalStateException("no connection")

        // When
        val result = useCase(BILL_ID)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario() {
        activity = FakeActivityRepository()
        useCase = LoadBillHistoryUseCase(activity)
    }

    private companion object {
        const val BILL_ID = "bill-1"
    }
}
