package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpenseRulesTest {
    @Test
    fun `GIVEN no amount WHEN checking it THEN it is invalid`() {
        // Given
        val amountMinor: Long? = null

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.BRL)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN an amount of 0 WHEN checking it THEN it is invalid`() {
        // Given
        val amountMinor = 0L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.BRL)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a negative amount WHEN checking it THEN it is invalid`() {
        // Given
        val amountMinor = -1L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.BRL)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN one cent WHEN checking it THEN it is valid`() {
        // Given
        val amountMinor = 1L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.BRL)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN exactly 100 million reais WHEN checking it THEN it is valid`() {
        // Given
        val amountMinor = 10_000_000_000L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.BRL)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN one cent over 100 million reais WHEN checking it THEN it is invalid`() {
        // Given
        val amountMinor = 10_000_000_001L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.BRL)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN exactly 100 million yen WHEN checking it THEN it is valid`() {
        // Given
        val amountMinor = 100_000_000L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.JPY)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN one yen over 100 million WHEN checking it THEN it is invalid`() {
        // Given
        val amountMinor = 100_000_001L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.JPY)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN one peso over 100 million WHEN checking it THEN it is invalid`() {
        // Given
        val amountMinor = 100_000_001L

        // When
        val isValid = ExpenseRules.isAmountValid(amountMinor, GroupCurrency.CLP)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN currencies with and without cents WHEN reading the limit THEN it is 100 million major units`() {
        // Given
        val currencies = listOf(GroupCurrency.BRL, GroupCurrency.USD, GroupCurrency.PYG, GroupCurrency.JPY)

        // When
        val limits = currencies.map(ExpenseRules::maxAmountMinorFor)

        // Then
        assertEquals(listOf(10_000_000_000L, 10_000_000_000L, 100_000_000L, 100_000_000L), limits)
    }

    @Test
    fun `GIVEN an empty description WHEN checking it THEN it is invalid`() {
        // Given
        val description = ""

        // When
        val isValid = ExpenseRules.isDescriptionValid(description)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN only spaces WHEN checking the description THEN it is invalid`() {
        // Given
        val description = "   "

        // When
        val isValid = ExpenseRules.isDescriptionValid(description)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN one letter between spaces WHEN checking the description THEN it is valid`() {
        // Given
        val description = " a "

        // When
        val isValid = ExpenseRules.isDescriptionValid(description)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN 101 characters WHEN capping the description THEN keeps the first 100`() {
        // Given
        val description = "a".repeat(100) + "b"

        // When
        val capped = ExpenseRules.toCapped(description)

        // Then
        assertEquals("a".repeat(100), capped)
    }

    @Test
    fun `GIVEN 100 characters WHEN capping the description THEN keeps it whole`() {
        // Given
        val description = "b".repeat(100)

        // When
        val capped = ExpenseRules.toCapped(description)

        // Then
        assertEquals(description, capped)
    }

    @Test
    fun `GIVEN an emoji across the 100th character WHEN capping the description THEN drops the whole emoji`() {
        // Given
        val description = "a".repeat(99) + "\uD83C\uDF55"

        // When
        val capped = ExpenseRules.toCapped(description)

        // Then
        assertEquals("a".repeat(99), capped)
    }
}
