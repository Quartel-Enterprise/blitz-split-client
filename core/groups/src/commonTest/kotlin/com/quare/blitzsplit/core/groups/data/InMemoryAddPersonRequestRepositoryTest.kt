package com.quare.blitzsplit.core.groups.data

import com.quare.blitzsplit.core.groups.data.repository.InMemoryAddPersonRequestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryAddPersonRequestRepositoryTest {
    private val repository = InMemoryAddPersonRequestRepository()

    @Test
    fun `GIVEN no ask posted WHEN observing THEN there is none`() = runTest {
        // Given
        val fresh = InMemoryAddPersonRequestRepository()

        // When
        val request = fresh.observeRequest().first()

        // Then
        assertNull(request)
    }

    @Test
    fun `GIVEN an ask for a group WHEN the ask of another group is consumed THEN it stays`() = runTest {
        // Given
        repository.post("group-1")

        // When
        repository.consume("group-2")

        // Then
        assertEquals("group-1", repository.observeRequest().first())
    }

    @Test
    fun `GIVEN an ask for a group WHEN it is consumed THEN there is none`() = runTest {
        // Given
        repository.post("group-1")

        // When
        repository.consume("group-1")

        // Then
        assertNull(repository.observeRequest().first())
    }
}
