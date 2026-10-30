package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillSplitTest {
    @Test
    fun `GIVEN three members in an equal split WHEN the total divides evenly THEN each pays the same`() {
        // Given
        val entries = listOf(entry(ANA, 1), entry(BRUNO, 1), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 900,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 300, 1), share(BRUNO, 300, 1), share(PIERRE, 300, 1)), shares)
    }

    @Test
    fun `GIVEN the payer in an equal split WHEN a cent is left over THEN the payer takes it`() {
        // Given
        val entries = listOf(entry(ANA, 1), entry(BRUNO, 1), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 333, 1), share(BRUNO, 333, 1), share(PIERRE, 334, 1)), shares)
    }

    @Test
    fun `GIVEN the payer out of an equal split WHEN two cents are left THEN the first two take one each`() {
        // Given
        val entries = listOf(entry(PIERRE, 0), entry(ANA, 1), entry(BRUNO, 1), entry(CARLA, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_001,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 334, 1), share(BRUNO, 334, 1), share(CARLA, 333, 1)), shares)
    }

    @Test
    fun `GIVEN a member left out of an equal split WHEN splitting THEN the member has no share`() {
        // Given
        val entries = listOf(entry(ANA, 1), entry(BRUNO, 0), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 500, 1), share(PIERRE, 500, 1)), shares)
    }

    @Test
    fun `GIVEN a member in an equal split with a value above 1 WHEN splitting THEN the member counts once`() {
        // Given
        val entries = listOf(entry(ANA, 3), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 500, 1), share(PIERRE, 500, 1)), shares)
    }

    @Test
    fun `GIVEN amounts that add up to the total WHEN splitting by amount THEN each pays what was typed`() {
        // Given
        val entries = listOf(entry(ANA, 650), entry(BRUNO, 0), entry(PIERRE, 350))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.AMOUNT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 650, 650), share(PIERRE, 350, 350)), shares)
    }

    @Test
    fun `GIVEN amounts below the total WHEN splitting by amount THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 600), entry(PIERRE, 399))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.AMOUNT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN amounts above the total WHEN splitting by amount THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 600), entry(PIERRE, 401))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.AMOUNT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN percentages adding up to 100 WHEN splitting by percentage THEN each pays that part of the total`() {
        // Given
        val entries = listOf(entry(ANA, 50), entry(BRUNO, 30), entry(PIERRE, 20))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.PERCENT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 500, 50), share(BRUNO, 300, 30), share(PIERRE, 200, 20)), shares)
    }

    @Test
    fun `GIVEN percentages that leave a cent over WHEN splitting by percentage THEN the payer takes it`() {
        // Given
        val entries = listOf(entry(ANA, 33), entry(BRUNO, 33), entry(PIERRE, 34))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_001,
            mode = SplitMode.PERCENT,
            entries = entries,
            payerId = BRUNO,
        )

        // Then
        assertEquals(listOf(share(ANA, 330, 33), share(BRUNO, 331, 33), share(PIERRE, 340, 34)), shares)
    }

    @Test
    fun `GIVEN percentages adding up to less than 100 WHEN splitting by percentage THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 50), entry(PIERRE, 40))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.PERCENT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN percentages adding up to more than 100 WHEN splitting by percentage THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 60), entry(PIERRE, 50))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.PERCENT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN parts of 2 1 and 1 WHEN splitting by parts THEN each pays in proportion to the parts`() {
        // Given
        val entries = listOf(entry(ANA, 2), entry(BRUNO, 1), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.SHARES,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(share(ANA, 500, 2), share(BRUNO, 250, 1), share(PIERRE, 250, 1)), shares)
    }

    @Test
    fun `GIVEN the payer in a split by parts WHEN a cent is left over THEN the payer takes it`() {
        // Given
        val entries = listOf(entry(PIERRE, 1), entry(ANA, 1), entry(BRUNO, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.SHARES,
            entries = entries,
            payerId = ANA,
        )

        // Then
        assertEquals(listOf(share(PIERRE, 333, 1), share(ANA, 334, 1), share(BRUNO, 333, 1)), shares)
    }

    @Test
    fun `GIVEN a total of 0 WHEN splitting THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 1), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = 0,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN a negative total WHEN splitting THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 1), entry(PIERRE, 1))

        // When
        val shares = BillSplit.split(
            totalMinor = -1_000,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN every member left out WHEN splitting THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 0), entry(PIERRE, 0))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.EQUAL,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN no members WHEN splitting THEN there is no split`() {
        // Given
        val entries = emptyList<SplitEntry>()

        // When
        val shares = BillSplit.split(
            totalMinor = 1_000,
            mode = SplitMode.SHARES,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN a negative value next to amounts that add up WHEN splitting by amount THEN there is no split`() {
        // Given
        val entries = listOf(entry(ANA, 1_100), entry(BRUNO, -100), entry(PIERRE, 0))

        // When
        val shares = BillSplit.split(
            totalMinor = 1_100,
            mode = SplitMode.AMOUNT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN 100 million in huge parts WHEN splitting by parts THEN the shares still add up`() {
        // Given
        val total = 10_000_000_000L
        val entries = listOf(
            entry(PIERRE, 0),
            entry(ANA, 999_999_999_999),
            entry(BRUNO, 999_999_999_998),
            entry(CARLA, 7),
        )

        // When
        val shares = BillSplit.split(
            totalMinor = total,
            mode = SplitMode.SHARES,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertNotNull(shares)
        assertEquals(total, shares.sumOf { it.amountMinor })
        assertTrue(shares.all { it.amountMinor >= 0 })
        assertEquals(listOf(5_000_000_000L, 5_000_000_000L, 0L), shares.map { it.amountMinor })
    }

    @Test
    fun `GIVEN 100 million split by amount WHEN splitting THEN each pays what was typed`() {
        // Given
        val entries = listOf(entry(ANA, 6_000_000_000), entry(PIERRE, 4_000_000_000))

        // When
        val shares = BillSplit.split(
            totalMinor = 10_000_000_000,
            mode = SplitMode.AMOUNT,
            entries = entries,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(6_000_000_000L, 4_000_000_000L), shares?.map { it.amountMinor })
    }

    @Test
    fun `GIVEN amounts that add up to the old total WHEN the total grows THEN they add up to the new one`() {
        // Given
        val entries = listOf(entry(ANA, 600), entry(PIERRE, 400))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 1_500,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(entry(ANA, 900), entry(PIERRE, 600)), rescaled)
    }

    @Test
    fun `GIVEN amounts that add up to the old total WHEN the new total leaves a cent over THEN the payer takes it`() {
        // Given
        val entries = listOf(entry(ANA, 333), entry(BRUNO, 333), entry(PIERRE, 334))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 1_001,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(entry(ANA, 333), entry(BRUNO, 333), entry(PIERRE, 335)), rescaled)
    }

    @Test
    fun `GIVEN the payer out of the amounts WHEN the new total leaves a cent over THEN the first member takes it`() {
        // Given
        val entries = listOf(entry(PIERRE, 0), entry(ANA, 500), entry(BRUNO, 500))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 1_001,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(entry(PIERRE, 0), entry(ANA, 501), entry(BRUNO, 500)), rescaled)
    }

    @Test
    fun `GIVEN amounts that do not add up to the old total WHEN the total doubles THEN each keeps its proportion`() {
        // Given
        val entries = listOf(entry(ANA, 300), entry(BRUNO, 0), entry(PIERRE, 201))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 2_000,
            payerId = PIERRE,
        )

        // Then
        assertEquals(listOf(entry(ANA, 600), entry(BRUNO, 0), entry(PIERRE, 402)), rescaled)
    }

    @Test
    fun `GIVEN amounts WHEN the total stays the same THEN they stay as typed`() {
        // Given
        val entries = listOf(entry(ANA, 300), entry(PIERRE, 200))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 1_000,
            payerId = PIERRE,
        )

        // Then
        assertEquals(entries, rescaled)
    }

    @Test
    fun `GIVEN amounts typed before any total WHEN the total is set THEN they stay as typed`() {
        // Given
        val entries = listOf(entry(ANA, 300), entry(PIERRE, 200))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 0,
            newTotalMinor = 1_000,
            payerId = PIERRE,
        )

        // Then
        assertEquals(entries, rescaled)
    }

    @Test
    fun `GIVEN amounts WHEN the total is cleared THEN they stay as typed`() {
        // Given
        val entries = listOf(entry(ANA, 600), entry(PIERRE, 400))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 0,
            payerId = PIERRE,
        )

        // Then
        assertEquals(entries, rescaled)
    }

    @Test
    fun `GIVEN every member at 0 WHEN the total changes THEN they stay at 0`() {
        // Given
        val entries = listOf(entry(ANA, 0), entry(PIERRE, 0))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 1_000,
            newTotalMinor = 2_000,
            payerId = PIERRE,
        )

        // Then
        assertEquals(entries, rescaled)
    }

    @Test
    fun `GIVEN amounts adding up to 100 million WHEN the total drops a cent THEN they add up to the new total`() {
        // Given
        val entries = listOf(entry(PIERRE, 0), entry(ANA, 6_000_000_000), entry(BRUNO, 4_000_000_000))

        // When
        val rescaled = BillSplit.calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = 10_000_000_000,
            newTotalMinor = 9_999_999_999,
            payerId = PIERRE,
        )

        // Then
        assertEquals(9_999_999_999L, rescaled.sumOf { it.value })
        assertEquals(listOf(0L, 6_000_000_000L, 3_999_999_999L), rescaled.map { it.value })
    }

    private fun entry(
        memberId: String,
        value: Long,
    ): SplitEntry = SplitEntry(
        memberId = memberId,
        value = value,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
        weight: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = weight,
    )

    private companion object {
        const val PIERRE = "pierre"
        const val ANA = "ana"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
    }
}
