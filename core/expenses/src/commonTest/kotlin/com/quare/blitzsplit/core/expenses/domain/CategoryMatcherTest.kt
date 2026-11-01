package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.DefaultCategoryTerms
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryMatcherTest {
    private val terms = DefaultCategoryTerms.terms

    @Test
    fun `GIVEN the default map WHEN counting its terms THEN there are 60, none twice`() {
        // Given
        val keys = terms.map { CategoryMatcher.keyOf(it.term) }

        // When
        val distinct = keys.toSet()

        // Then
        assertEquals(60, keys.size)
        assertEquals(60, distinct.size)
        assertTrue(terms.all { AppCategory.fromKeyOrNull(it.categoryId) != null })
    }

    @Test
    fun `GIVEN Mercado WHEN matching THEN Alimentação › Mercado by mercado, marking where it is`() {
        // Given
        val title = "Mercado"

        // When
        val match = CategoryMatcher.matchOrNull(title, terms)

        // Then
        assertEquals(AppCategory.FOOD_MARKET.key, match?.categoryId)
        assertEquals(0 to 7, match?.let { it.start to it.end })
    }

    @Test
    fun `GIVEN several terms in the title WHEN matching THEN the longest wins`() {
        // Given
        val title = "Uber Eats no hotel"

        // When
        val match = CategoryMatcher.matchOrNull(title, terms)

        // Then
        assertEquals("uber eats", match?.term)
        assertEquals(AppCategory.FOOD_DELIVERY.key, match?.categoryId)
    }

    @Test
    fun `GIVEN two terms as long WHEN matching THEN the first in the title wins`() {
        // Given
        val title = "Luz e gás"

        // When
        val match = CategoryMatcher.matchOrNull(title, terms)

        // Then
        assertEquals("luz", match?.term)
    }

    @Test
    fun `GIVEN accents and case that differ WHEN matching THEN still matches, but never inside a word`() {
        // Given
        val accentless = "FARMACIA do centro"
        val inside = "Barbearia"

        // When
        val pharmacy = CategoryMatcher.matchOrNull(accentless, terms)
        val barber = CategoryMatcher.matchOrNull(inside, terms)

        // Then
        assertEquals(AppCategory.HEALTH_PHARMACY.key, pharmacy?.categoryId)
        assertNull(barber)
    }

    @Test
    fun `GIVEN no term in the title WHEN matching THEN nothing`() {
        // Given
        val title = "Rifa da formatura"

        // When
        val match = CategoryMatcher.matchOrNull(title, terms)

        // Then
        assertNull(match)
    }

    @Test
    fun `GIVEN terms typed WHEN validating THEN 2 to 30 letters, digits and spaces`() {
        // Given
        val valid = listOf("99", "uber eats", "pão")
        val invalid = listOf("a", " ", "uber!", "a".repeat(31))

        // When
        val results = valid.map(CategoryMatcher::isTermValid) + invalid.map(CategoryMatcher::isTermValid)

        // Then
        assertEquals(List(valid.size) { true } + List(invalid.size) { false }, results)
        assertFalse(CategoryMatcher.isTermValid("  a "))
    }
}
