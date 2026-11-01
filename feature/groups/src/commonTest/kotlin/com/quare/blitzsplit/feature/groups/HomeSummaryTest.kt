package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSummaryTest {
    @Test
    fun `GIVEN only the second currency has a net WHEN reading Saldo geral THEN it is to receive`() {
        // Given
        val summary = HomeSummary(
            totals = listOf(
                total(GroupCurrency.BRL),
                total(
                    GroupCurrency.USD,
                    toReceiveMinor = 400L,
                ),
            ),
        )

        // When
        val kind = summary.kind

        // Then
        assertEquals(GroupBalanceKind.TO_RECEIVE, kind)
    }

    @Test
    fun `GIVEN you owe in the first currency and are owed in the second WHEN reading Saldo geral THEN it is to pay`() {
        // Given
        val summary = HomeSummary(
            totals = listOf(
                total(
                    GroupCurrency.BRL,
                    toPayMinor = 3000L,
                ),
                total(
                    GroupCurrency.USD,
                    toReceiveMinor = 400L,
                ),
            ),
        )

        // When
        val kind = summary.kind

        // Then
        assertEquals(GroupBalanceKind.YOU_OWE, kind)
    }

    @Test
    fun `GIVEN people who offset each other WHEN reading Saldo geral THEN it is settled with both buttons enabled`() {
        // Given
        val summary = HomeSummary(
            totals = listOf(
                total(
                    GroupCurrency.BRL,
                    toReceiveMinor = 500L,
                    toPayMinor = 500L,
                ),
            ),
        )

        // When
        val kind = summary.kind

        // Then
        assertEquals(GroupBalanceKind.YOU_ARE_SETTLED, kind)
        assertTrue(summary.canOpenToReceive)
        assertTrue(summary.canOpenToPay)
    }

    @Test
    fun `GIVEN no group WHEN reading Saldo geral THEN it is settled with both buttons disabled`() {
        // Given
        val summary = HomeSummary(totals = emptyList())

        // When
        val kind = summary.kind

        // Then
        assertEquals(GroupBalanceKind.YOU_ARE_SETTLED, kind)
        assertFalse(summary.canOpenToReceive)
        assertFalse(summary.canOpenToPay)
    }

    @Test
    fun `GIVEN something to receive only in the second currency WHEN reading the buttons THEN only Te devem works`() {
        // Given
        val summary = HomeSummary(
            totals = listOf(
                total(GroupCurrency.BRL),
                total(
                    GroupCurrency.USD,
                    toReceiveMinor = 400L,
                ),
            ),
        )

        // When
        val buttons = summary.canOpenToReceive to summary.canOpenToPay

        // Then
        assertEquals(true to false, buttons)
    }

    @Test
    fun `GIVEN something to pay only in the second currency WHEN reading the buttons THEN only Voce deve works`() {
        // Given
        val summary = HomeSummary(
            totals = listOf(
                total(GroupCurrency.BRL),
                total(
                    GroupCurrency.USD,
                    toPayMinor = 250L,
                ),
            ),
        )

        // When
        val buttons = summary.canOpenToReceive to summary.canOpenToPay

        // Then
        assertEquals(false to true, buttons)
    }

    @Test
    fun `GIVEN a first currency settled and two with a net WHEN reading Saldo geral THEN the first with one is big`() {
        // Given
        val brl = total(GroupCurrency.BRL)
        val usd = total(GroupCurrency.USD, toPayMinor = 250L)
        val eur = total(GroupCurrency.EUR, toReceiveMinor = 900L)
        val summary = HomeSummary(totals = listOf(brl, usd, eur))

        // When
        val amounts = summary.main to summary.others

        // Then
        assertEquals(usd to listOf(eur), amounts)
    }

    @Test
    fun `GIVEN every currency settled WHEN reading Saldo geral THEN the first is big with no other line`() {
        // Given
        val brl = total(GroupCurrency.BRL)
        val summary = HomeSummary(totals = listOf(brl, total(GroupCurrency.USD)))

        // When
        val amounts = summary.main to summary.others

        // Then
        assertEquals(brl to emptyList(), amounts)
    }

    @Test
    fun `GIVEN each way owed in another currency WHEN reading the buttons THEN each shows its own currency`() {
        // Given
        val brl = total(GroupCurrency.BRL, toPayMinor = 3000L)
        val usd = total(GroupCurrency.USD, toReceiveMinor = 400L)
        val summary = HomeSummary(totals = listOf(brl, usd))

        // When
        val buttons = summary.toReceive to summary.toPay

        // Then
        assertEquals(usd to brl, buttons)
    }

    @Test
    fun `GIVEN no group WHEN reading Saldo geral THEN has no amount at all`() {
        // Given
        val summary = HomeSummary(totals = emptyList())

        // When
        val amounts = listOf(summary.main, summary.toReceive, summary.toPay)

        // Then
        assertEquals(listOf(null, null, null), amounts)
        assertTrue(summary.others.isEmpty())
    }

    private fun total(
        currency: GroupCurrency,
        toReceiveMinor: Long = 0L,
        toPayMinor: Long = 0L,
    ): PeopleTotal = PeopleTotal(
        currency = currency,
        netMinor = toReceiveMinor - toPayMinor,
        toReceiveMinor = toReceiveMinor,
        toPayMinor = toPayMinor,
        toReceiveCount = if (toReceiveMinor > 0) 1 else 0,
        toPayCount = if (toPayMinor > 0) 1 else 0,
    )
}
