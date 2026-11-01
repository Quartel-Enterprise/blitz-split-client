package com.quare.blitzsplit.core.contacts.domain

import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.CancelGuestMergeUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeGuestMergeRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CancelGuestMergeUseCaseTest {
    private lateinit var repository: FakeGuestMergeRepository
    private lateinit var useCase: CancelGuestMergeUseCase

    @Test
    fun `GIVEN a request waiting for the guest WHEN cancelling it THEN it no longer waits`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = useCase("merge-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("merge-1"), repository.cancelledIds)
        assertNull(repository.pending)
    }

    @Test
    fun `GIVEN the server fails WHEN cancelling a request THEN returns that failure`() = runTest {
        // Given
        prepareScenario(cancelError = IllegalStateException("offline"))

        // When
        val result = useCase("merge-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    private fun prepareScenario(cancelError: Throwable? = null) {
        repository = FakeGuestMergeRepository(
            pending = GuestMerge(
                id = "merge-1",
                memberIds = listOf("diego-ape"),
                link = "https://blitzsplit.com/g/Code1234/dg1234",
                requestedAt = 100L,
            ),
        ).also { it.cancelError = cancelError }
        useCase = CancelGuestMergeUseCase(repository)
    }
}
