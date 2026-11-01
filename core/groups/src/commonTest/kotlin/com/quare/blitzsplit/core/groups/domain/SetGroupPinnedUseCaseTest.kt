package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SetGroupPinnedUseCaseTest {
    private lateinit var repository: FakeGroupPinRepository
    private lateinit var useCase: SetGroupPinnedUseCase

    @Test
    fun `GIVEN the clock at a time WHEN pinning a group THEN saves the pin stamped with that time`() = runTest {
        // Given
        prepareScenario(now = 1_000L)

        // When
        val result = useCase(GROUP_ID, isPinned = true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf(Triple(GROUP_ID, true, 1_000L)), repository.pinUpdates)
        assertEquals(setOf(GROUP_ID), repository.pinnedGroupIds.value)
    }

    @Test
    fun `GIVEN a pinned group WHEN unpinning it THEN saves the unpin stamped with the current time`() = runTest {
        // Given
        prepareScenario(
            now = 2_000L,
            pinned = setOf(GROUP_ID),
        )

        // When
        useCase(GROUP_ID, isPinned = false)

        // Then
        assertEquals(listOf(Triple(GROUP_ID, false, 2_000L)), repository.pinUpdates)
        assertTrue(repository.pinnedGroupIds.value.isEmpty())
    }

    @Test
    fun `GIVEN saving on the device fails WHEN pinning a group THEN gives back the failure`() = runTest {
        // Given
        prepareScenario(updateError = IllegalStateException("disk full"))

        // When
        val result = useCase(GROUP_ID, isPinned = true)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(repository.pinnedGroupIds.value.isEmpty())
    }

    private fun prepareScenario(
        now: Long = 1_000L,
        pinned: Set<String> = emptySet(),
        updateError: Throwable? = null,
    ) {
        repository = FakeGroupPinRepository(initialPinnedGroupIds = pinned).also {
            it.updatePinnedError = updateError
        }
        useCase = SetGroupPinnedUseCase(repository) { now }
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
