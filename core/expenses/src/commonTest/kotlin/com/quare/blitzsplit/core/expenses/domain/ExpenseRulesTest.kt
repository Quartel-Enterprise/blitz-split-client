package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
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

    @Test
    fun `GIVEN a payer of the whole bill who is also in the split WHEN checking the split THEN it is valid`() {
        // Given
        val payers = listOf(payer(PIERRE, 1_000L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN several payers adding up to the bill WHEN checking the split THEN it is valid`() {
        // Given
        val payers = listOf(payer(ANA, 400L), payer(PIERRE, 600L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN no payer WHEN checking the split THEN it is invalid`() {
        // Given
        val payers = emptyList<ExpensePayer>()

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN no share WHEN checking the split THEN it is invalid`() {
        // Given
        val shares = emptyList<ExpenseShare>()

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN the same member twice among the payers WHEN checking the split THEN it is invalid`() {
        // Given
        val payers = listOf(payer(PIERRE, 500L), payer(PIERRE, 500L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN the same member twice among the shares WHEN checking the split THEN it is invalid`() {
        // Given
        val shares = listOf(share(ANA, 500L), share(ANA, 500L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a negative payer amount the payers still adding up WHEN checking the split THEN it is invalid`() {
        // Given
        val payers = listOf(payer(PIERRE, 1_100L), payer(ANA, -100L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a negative share amount the shares still adding up WHEN checking the split THEN it is invalid`() {
        // Given
        val shares = listOf(share(PIERRE, 1_100L, weight = 1_100L), share(ANA, -100L, weight = 0L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.AMOUNT, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN a negative weight WHEN checking the split THEN it is invalid`() {
        // Given
        val shares = listOf(share(PIERRE, 500L, weight = 2L), share(ANA, 500L, weight = -1L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.SHARES, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN payers adding up to one cent less than the bill WHEN checking the split THEN it is invalid`() {
        // Given
        val payers = listOf(payer(PIERRE, 600L), payer(ANA, 399L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN payers adding up to one cent more than the bill WHEN checking the split THEN it is invalid`() {
        // Given
        val payers = listOf(payer(PIERRE, 1_001L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN shares adding up to one cent less than the bill WHEN checking the split THEN it is invalid`() {
        // Given
        val shares = listOf(share(PIERRE, 500L), share(ANA, 499L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN shares adding up to one cent more than the bill WHEN checking the split THEN it is invalid`() {
        // Given
        val shares = listOf(share(PIERRE, 501L), share(ANA, 500L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN percentages adding up to 100 WHEN checking a percent split THEN it is valid`() {
        // Given
        val shares = listOf(share(PIERRE, 300L, weight = 30L), share(ANA, 700L, weight = 70L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.PERCENT, wholeBillByPierre, shares, null)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN percentages adding up to 99 WHEN checking a percent split THEN it is invalid`() {
        // Given
        val shares = listOf(share(PIERRE, 300L, weight = 29L), share(ANA, 700L, weight = 70L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.PERCENT, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN percentages adding up to 101 WHEN checking a percent split THEN it is invalid`() {
        // Given
        val shares = listOf(share(PIERRE, 300L, weight = 31L), share(ANA, 700L, weight = 70L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.PERCENT, wholeBillByPierre, shares, null)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN weights not adding up to 100 WHEN checking an equal amount or parts split THEN it is valid`() {
        // Given
        val shares = listOf(share(PIERRE, 333L, weight = 1L), share(ANA, 667L, weight = 2L))
        val modes = listOf(SplitMode.EQUAL, SplitMode.AMOUNT, SplitMode.SHARES)

        // When
        val validity = modes.map { ExpenseRules.isSplitValid(1_000L, it, wholeBillByPierre, shares, null) }

        // Then
        assertEquals(listOf(true, true, true), validity)
    }

    @Test
    fun `GIVEN a payer who paid 0 WHEN checking the split THEN it is valid`() {
        // Given
        val payers = listOf(payer(PIERRE, 1_000L), payer(ANA, 0L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.EQUAL, payers, halves, null)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN a share of 0 at 0 percent WHEN checking a percent split THEN it is valid`() {
        // Given
        val shares = listOf(share(PIERRE, 1_000L, weight = 100L), share(ANA, 0L, weight = 0L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.PERCENT, wholeBillByPierre, shares, null)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN a bill split by its items with the shares they give WHEN checking the split THEN it is valid`() {
        // Given
        val shares = listOf(share(PIERRE, 500L, weight = 500L), share(ANA, 500L, weight = 500L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.ITEMS, wholeBillByPierre, shares, pizzaForTwo)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `GIVEN items whose shares are not the ones saved WHEN checking the split THEN it is not valid`() {
        // Given
        val shares = listOf(share(PIERRE, 600L, weight = 600L), share(ANA, 400L, weight = 400L))

        // When
        val isValid = ExpenseRules.isSplitValid(1_000L, SplitMode.ITEMS, wholeBillByPierre, shares, pizzaForTwo)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `GIVEN items on a bill split another way or none by items WHEN checking the split THEN it is not valid`() {
        // Given
        val shares = listOf(share(PIERRE, 500L, weight = 500L), share(ANA, 500L, weight = 500L))

        // When
        val withItems = ExpenseRules.isSplitValid(1_000L, SplitMode.AMOUNT, wholeBillByPierre, shares, pizzaForTwo)
        val withoutItems = ExpenseRules.isSplitValid(1_000L, SplitMode.ITEMS, wholeBillByPierre, shares, null)

        // Then
        assertFalse(withItems)
        assertFalse(withoutItems)
    }

    private val pizzaForTwo = BillItems(
        items = listOf(
            BillItem(
                id = "pizza",
                name = "Pizza",
                quantity = 1L,
                unitPriceMinor = 1_000L,
                mode = ItemSplitMode.EQUAL,
                entries = listOf(
                    SplitEntry(
                        memberId = PIERRE,
                        value = 1L,
                    ),
                    SplitEntry(
                        memberId = ANA,
                        value = 1L,
                    ),
                ),
            ),
        ),
        servicePercent = 0L,
        couvertMinor = 0L,
        couvertMemberIds = listOf(PIERRE, ANA),
        extraMinor = 0L,
        isFromReceipt = false,
    )

    private val wholeBillByPierre = listOf(payer(PIERRE, 1_000L))

    private val halves = listOf(share(PIERRE, 500L), share(ANA, 500L))

    private fun payer(
        memberId: String,
        amountMinor: Long,
    ): ExpensePayer = ExpensePayer(
        memberId = memberId,
        amountMinor = amountMinor,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
        weight: Long = 1L,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = weight,
    )

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
    }
}
