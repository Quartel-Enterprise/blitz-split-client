package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryMapTest {
    @Test
    fun `GIVEN no change WHEN building the map THEN it has the app 60 terms none pending`() {
        // Given
        val changes = emptyList<CategoryTermChange>()

        // When
        val map = CategoryMap.of(changes)

        // Then
        assertEquals(60, map.terms.size)
        assertTrue(map.terms.all { it.isAppTerm && !it.isPending })
        assertEquals(0, map.pendingCount)
    }

    @Test
    fun `GIVEN a term added one moved and one removed WHEN building the map THEN each counts as changed`() {
        // Given
        val changes = listOf(
            changeOf("picanha", "group-category-1", isPending = true),
            changeOf("uber", AppCategory.LEISURE_PARTIES.key),
            changeOf("café", null, isPending = true),
        )

        // When
        val map = CategoryMap.of(changes)

        // Then
        val added = map.findOrNull("picanha")
        assertEquals("group-category-1", added?.categoryId)
        assertFalse(added?.isAppTerm ?: true)
        assertEquals(AppCategory.LEISURE_PARTIES.key, map.findOrNull("uber")?.categoryId)
        assertTrue(map.findOrNull("uber")?.isAppTerm ?: false)
        assertNull(map.findOrNull("cafe"))
        assertEquals(60, map.terms.size)
        assertEquals(2, map.pendingCount)
    }

    @Test
    fun `GIVEN changes restored WHEN building the map THEN the app terms are back as they came`() {
        // Given
        val changes = listOf(
            changeOf("uber", AppCategory.LEISURE_PARTIES.key, isRestored = true),
            changeOf(
                "café",
                null,
                isRestored = true,
                isPending = true,
            ),
        )

        // When
        val map = CategoryMap.of(changes)

        // Then
        assertEquals(AppCategory.TRANSPORT_RIDE.key, map.findOrNull("uber")?.categoryId)
        assertEquals(AppCategory.FOOD_BAKERY.key, map.findOrNull("cafe")?.categoryId)
        assertTrue(map.findOrNull("cafe")?.isPending ?: false)
    }

    private fun changeOf(
        term: String,
        categoryId: String?,
        isRestored: Boolean = false,
        isPending: Boolean = false,
    ): CategoryTermChange = CategoryTermChange(
        key = com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
            .keyOf(term),
        term = term,
        categoryId = categoryId,
        isRestored = isRestored,
        isPending = isPending,
    )
}
