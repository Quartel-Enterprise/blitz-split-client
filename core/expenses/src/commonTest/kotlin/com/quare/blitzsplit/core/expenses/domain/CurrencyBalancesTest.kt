package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.currencies
import com.quare.blitzsplit.core.expenses.domain.model.findSelected
import com.quare.blitzsplit.core.expenses.domain.model.hasBills
import com.quare.blitzsplit.core.expenses.domain.model.isEveryoneSettled
import com.quare.blitzsplit.core.expenses.domain.model.openDebts
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrencyBalancesTest {
    private val reais = CurrencyBalances(GroupCurrency.BRL, balances(Debt(ANA, BRUNO, 100L)))
    private val dollars = CurrencyBalances(GroupCurrency.USD, balances(Debt(BRUNO, ANA, 700L)))

    @Test
    fun `GIVEN two currencies WHEN picking one of them THEN returns its balance`() {
        // Given
        val byCurrency = listOf(reais, dollars)

        // When
        val selected = byCurrency.findSelected(GroupCurrency.USD)

        // Then
        assertEquals(dollars, selected)
    }

    @Test
    fun `GIVEN two currencies WHEN nothing is picked THEN falls back to the first which is the default`() {
        // Given
        val byCurrency = listOf(reais, dollars)

        // When
        val selected = byCurrency.findSelected(null)

        // Then
        assertEquals(reais, selected)
    }

    @Test
    fun `GIVEN a pick the group no longer has money in WHEN selecting THEN falls back to the first`() {
        // Given
        val byCurrency = listOf(reais, dollars)

        // When
        val selected = byCurrency.findSelected(GroupCurrency.EUR)

        // Then
        assertEquals(reais, selected)
    }

    @Test
    fun `GIVEN no balances at all WHEN selecting a currency THEN returns nothing`() {
        // Given
        val byCurrency = emptyList<CurrencyBalances>()

        // When
        val selected = byCurrency.findSelected(GroupCurrency.BRL)

        // Then
        assertNull(selected)
    }

    @Test
    fun `GIVEN two currencies WHEN listing the currencies THEN keeps their order`() {
        // Given
        val byCurrency = listOf(dollars, reais)

        // When
        val currencies = byCurrency.currencies

        // Then
        assertEquals(listOf(GroupCurrency.USD, GroupCurrency.BRL), currencies)
    }

    @Test
    fun `GIVEN a bill in only one of the currencies WHEN asking if the group has bills THEN it does`() {
        // Given
        val byCurrency = listOf(
            CurrencyBalances(GroupCurrency.BRL, balances(hasBills = false)),
            CurrencyBalances(GroupCurrency.USD, balances(hasBills = true)),
        )

        // When
        val hasBills = byCurrency.hasBills

        // Then
        assertTrue(hasBills)
    }

    @Test
    fun `GIVEN no bill in any currency WHEN asking if the group has bills THEN it does not`() {
        // Given
        val byCurrency = listOf(
            CurrencyBalances(GroupCurrency.BRL, balances(hasBills = false)),
            CurrencyBalances(GroupCurrency.USD, balances(hasBills = false)),
        )

        // When
        val hasBills = byCurrency.hasBills

        // Then
        assertFalse(hasBills)
    }

    @Test
    fun `GIVEN every currency settled with a bill WHEN asking if everyone settled THEN they did`() {
        // Given
        val byCurrency = listOf(
            CurrencyBalances(GroupCurrency.BRL, balances()),
            CurrencyBalances(GroupCurrency.USD, balances()),
        )

        // When
        val isEveryoneSettled = byCurrency.isEveryoneSettled

        // Then
        assertTrue(isEveryoneSettled)
    }

    @Test
    fun `GIVEN one currency still open WHEN asking if everyone settled THEN they did not`() {
        // Given
        val byCurrency = listOf(CurrencyBalances(GroupCurrency.BRL, balances()), dollars)

        // When
        val isEveryoneSettled = byCurrency.isEveryoneSettled

        // Then
        assertFalse(isEveryoneSettled)
    }

    @Test
    fun `GIVEN no bill in any currency WHEN asking if everyone settled THEN nobody did because nothing was owed`() {
        // Given
        val byCurrency = listOf(CurrencyBalances(GroupCurrency.BRL, balances(hasBills = false)))

        // When
        val isEveryoneSettled = byCurrency.isEveryoneSettled

        // Then
        assertFalse(isEveryoneSettled)
    }

    @Test
    fun `GIVEN no currency at all WHEN asking if everyone settled THEN nobody did`() {
        // Given
        val byCurrency = emptyList<CurrencyBalances>()

        // When
        val isEveryoneSettled = byCurrency.isEveryoneSettled

        // Then
        assertFalse(isEveryoneSettled)
    }

    @Test
    fun `GIVEN pairs in two currencies WHEN listing the open debts THEN lists every pair with its own currency`() {
        // Given
        val byCurrency = listOf(
            CurrencyBalances(GroupCurrency.BRL, balances(Debt(ANA, BRUNO, 100L), Debt(BRUNO, ANA, 50L))),
            dollars,
        )

        // When
        val openDebts = byCurrency.openDebts

        // Then
        assertEquals(
            listOf(
                GroupCurrency.BRL to Debt(ANA, BRUNO, 100L),
                GroupCurrency.BRL to Debt(BRUNO, ANA, 50L),
                GroupCurrency.USD to Debt(BRUNO, ANA, 700L),
            ),
            openDebts,
        )
    }

    @Test
    fun `GIVEN the same pair in two currencies WHEN listing the open debts THEN keeps both and never adds them`() {
        // Given
        val byCurrency = listOf(
            CurrencyBalances(GroupCurrency.BRL, balances(Debt(ANA, BRUNO, 100L))),
            CurrencyBalances(GroupCurrency.USD, balances(Debt(ANA, BRUNO, 100L))),
        )

        // When
        val openDebts = byCurrency.openDebts

        // Then
        assertEquals(
            listOf(GroupCurrency.BRL to Debt(ANA, BRUNO, 100L), GroupCurrency.USD to Debt(ANA, BRUNO, 100L)),
            openDebts,
        )
    }

    @Test
    fun `GIVEN every currency settled WHEN listing the open debts THEN lists none`() {
        // Given
        val byCurrency = listOf(CurrencyBalances(GroupCurrency.BRL, balances()))

        // When
        val openDebts = byCurrency.openDebts

        // Then
        assertTrue(openDebts.isEmpty())
    }

    private fun balances(
        vararg debts: Debt,
        hasBills: Boolean = true,
    ): GroupBalances = GroupBalances(
        debts = debts.toList(),
        hasBills = hasBills,
        unsimplifiedDebts = null,
    )

    private companion object {
        const val ANA = "ana"
        const val BRUNO = "bruno"
    }
}
