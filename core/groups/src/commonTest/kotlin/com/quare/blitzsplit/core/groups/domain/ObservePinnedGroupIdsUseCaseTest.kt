package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObservePinnedGroupIdsUseCaseTest {
    private val repository = FakeGroupPinRepository(initialPinnedGroupIds = setOf("group-1", "group-2"))
    private val useCase = ObservePinnedGroupIdsUseCase(repository)

    @Test
    fun `GIVEN two pinned groups WHEN observing the pins THEN gives both`() = runTest {
        // Given
        val expected = setOf("group-1", "group-2")

        // When
        val pinned = useCase().first()

        // Then
        assertEquals(expected, pinned)
    }

    @Test
    fun `GIVEN the pins observed WHEN a group is unpinned THEN gives the pins without it`() = runTest {
        // Given
        val emissions = mutableListOf<Set<String>>()
        val collecting = launch { useCase().take(2).toList(emissions) }
        runCurrent()

        // When
        repository.pinnedGroupIds.value = setOf("group-2")
        collecting.join()

        // Then
        assertEquals(listOf(setOf("group-1", "group-2"), setOf("group-2")), emissions)
    }
}
