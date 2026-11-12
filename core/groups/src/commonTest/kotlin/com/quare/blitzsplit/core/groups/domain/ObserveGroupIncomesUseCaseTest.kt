package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupIncomesUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupIncomeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveGroupIncomesUseCaseTest {
    private val repository = FakeGroupIncomeRepository(
        initialIncomes = mapOf(
            GROUP_1 to mapOf("a" to 900_000L, "b" to 300_000L),
            GROUP_2 to mapOf("c" to 500_000L),
        ),
    )
    private val useCase = ObserveGroupIncomesUseCase(repository)

    @Test
    fun `GIVEN incomes of two groups WHEN observing one group THEN gives only that group incomes`() = runTest {
        // Given
        val groupId = GROUP_1

        // When
        val incomes = useCase(groupId).first()

        // Then
        assertEquals(mapOf("a" to 900_000L, "b" to 300_000L), incomes)
    }

    @Test
    fun `GIVEN the incomes observed WHEN a member income is saved THEN gives the new incomes`() = runTest {
        // Given
        val emissions = mutableListOf<Map<String, Long>>()
        val collecting = launch { useCase(GROUP_1).take(2).toList(emissions) }
        runCurrent()

        // When
        repository.saveIncomes(GROUP_1, mapOf("b" to 0L), updatedAt = 1_000L)
        collecting.join()

        // Then
        assertEquals(listOf(mapOf("a" to 900_000L, "b" to 300_000L), mapOf("a" to 900_000L)), emissions)
    }

    private companion object {
        const val GROUP_1 = "group-1"
        const val GROUP_2 = "group-2"
    }
}
