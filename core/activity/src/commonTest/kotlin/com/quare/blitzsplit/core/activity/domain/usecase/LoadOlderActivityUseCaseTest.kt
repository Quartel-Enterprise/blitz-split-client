package com.quare.blitzsplit.core.activity.domain.usecase

import com.quare.blitzsplit.core.activity.domain.usecase.impl.LoadOlderActivityUseCase
import com.quare.blitzsplit.core.activity.testing.FakeActivityRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoadOlderActivityUseCaseTest {
    private lateinit var activity: FakeActivityRepository
    private lateinit var useCase: LoadOlderActivityUseCase

    @Test
    fun `GIVEN the feed WHEN loading older activity THEN loads the next older page`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, activity.loadOlderCalls)
    }

    @Test
    fun `GIVEN no connection WHEN loading older activity THEN returns the failure`() = runTest {
        // Given
        prepareScenario()
        activity.loadOlderError = IllegalStateException("no connection")

        // When
        val result = useCase()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario() {
        activity = FakeActivityRepository()
        useCase = LoadOlderActivityUseCase(activity)
    }
}
