package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkActivityReadUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkActivityReadUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: MarkActivityReadUseCase

    @Test
    fun `GIVEN an unread event WHEN opening it THEN it is read`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase("event-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(setOf("event-1"), activity.readState.value.readEventIds)
    }

    @Test
    fun `GIVEN the device fails to save WHEN opening an event THEN returns the failure`() = runTest {
        // Given
        prepareScenario()
        activity.markReadError = IllegalStateException("database closed")

        // When
        val result = useCase("event-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario() {
        activity = FakeActivityRepository()
        useCase = MarkActivityReadUseCase(activity)
    }
}
