package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateDefaultSplitUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.DefaultSplitMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateDefaultSplitUseCaseTest {
    private val repository = FakeGroupRepository()
    private val useCase = UpdateDefaultSplitUseCase(repository) { 1_000L }

    @Test
    fun `GIVEN Igual with everyone in WHEN saving THEN the group goes back to having no default`() = runTest {
        // Given
        val split = GroupDefaultSplit(DefaultSplitMode.EQUAL, mapOf("a" to 1L, "b" to 1L), emptyList())

        // When
        useCase(GROUP_ID, split, mapOf("a" to 100L))

        // Then
        assertNull(repository.savedDefaultSplits.single().second)
        assertTrue(repository.savedIncomes.getValue(GROUP_ID).isEmpty())
    }

    @Test
    fun `GIVEN Igual with someone out WHEN saving THEN it is kept as the default`() = runTest {
        // Given
        val split = GroupDefaultSplit(DefaultSplitMode.EQUAL, mapOf("a" to 1L, "b" to 0L), emptyList())

        // When
        useCase(GROUP_ID, split, emptyMap())

        // Then
        assertEquals(split, repository.savedDefaultSplits.single().second)
    }

    @Test
    fun `GIVEN Pela renda WHEN saving THEN the incomes typed stay along`() = runTest {
        // Given
        val split = GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf("a" to 75L, "b" to 25L), emptyList())

        // When
        useCase(GROUP_ID, split, mapOf("a" to 900_000L, "b" to 300_000L, "c" to 0L))

        // Then
        assertEquals(mapOf("a" to 900_000L, "b" to 300_000L), repository.savedIncomes[GROUP_ID])
    }

    @Test
    fun `GIVEN Partes WHEN saving THEN no income stays on the device`() = runTest {
        // Given
        val split = GroupDefaultSplit(DefaultSplitMode.SHARES, mapOf("a" to 3L, "b" to 1L), emptyList())

        // When
        useCase(GROUP_ID, split, mapOf("a" to 900_000L))

        // Then
        assertEquals(split, repository.savedDefaultSplits.single().second)
        assertTrue(repository.savedIncomes.getValue(GROUP_ID).isEmpty())
    }

    @Test
    fun `GIVEN Igual with everyone in and an exception WHEN saving THEN it is kept as the default`() = runTest {
        // Given
        val split = GroupDefaultSplit(
            mode = DefaultSplitMode.EQUAL,
            weights = mapOf("a" to 1L, "b" to 1L),
            exceptions = listOf(CategorySplit("food.market", DefaultSplitMode.SHARES, mapOf("a" to 2L, "b" to 1L))),
        )

        // When
        useCase(GROUP_ID, split, emptyMap())

        // Then
        assertEquals(split, repository.savedDefaultSplits.single().second)
    }

    @Test
    fun `GIVEN exceptions of a category and its main WHEN asking for the category THEN its own wins over the main`() {
        // Given
        val market = CategorySplit("food.market", DefaultSplitMode.EQUAL, mapOf("a" to 1L))
        val food = CategorySplit("food", DefaultSplitMode.SHARES, mapOf("a" to 2L))
        val split = GroupDefaultSplit(DefaultSplitMode.SHARES, mapOf("a" to 3L), listOf(food, market))

        // When
        val forMarket = split.exceptionFor(listOf("food.market", "food"))
        val forBar = split.exceptionFor(listOf("food.bar", "food"))
        val forNone = split.exceptionFor(listOf(null))

        // Then
        assertEquals(market, forMarket)
        assertEquals(food, forBar)
        assertNull(forNone)
    }

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
