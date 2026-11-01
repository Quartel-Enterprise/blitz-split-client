package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BillItemsSplitTest {
    @Test
    fun `GIVEN six beers by units WHEN splitting the item THEN each pays the units they had`() {
        // Given
        val beers = item(
            quantity = 6,
            unitPriceMinor = 1_400,
            mode = ItemSplitMode.UNITS,
            entries = listOf(entry(PIERRE, 2), entry(ANA, 1), entry(BRUNO, 3)),
        )

        // When
        val shares = BillItemsSplit.itemSharesOf(beers, payerIds = listOf(BRUNO))

        // Then
        assertEquals(listOf(share(PIERRE, 2_800, 2), share(ANA, 1_400, 1), share(BRUNO, 4_200, 3)), shares)
    }

    @Test
    fun `GIVEN units that do not add up to the quantity WHEN splitting the item THEN it does not close`() {
        // Given
        val beers = item(
            quantity = 6,
            unitPriceMinor = 1_400,
            mode = ItemSplitMode.UNITS,
            entries = listOf(entry(PIERRE, 2), entry(ANA, 3)),
        )

        // When
        val shares = BillItemsSplit.itemSharesOf(beers, payerIds = listOf(BRUNO))

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN an item split equally WHEN a cent is left over THEN whoever paid takes it while they had some`() {
        // Given
        val water = item(
            quantity = 1,
            unitPriceMinor = 1_000,
            mode = ItemSplitMode.EQUAL,
            entries = listOf(entry(PIERRE, 1), entry(ANA, 1), entry(BRUNO, 1)),
        )

        // When
        val shares = BillItemsSplit.itemSharesOf(water, payerIds = listOf(ANA))

        // Then
        assertEquals(listOf(share(PIERRE, 333, 1), share(ANA, 334, 1), share(BRUNO, 333, 1)), shares)
    }

    @Test
    fun `GIVEN an item nobody consumed WHEN splitting it THEN it does not close`() {
        // Given
        val loose = item(
            quantity = 1,
            unitPriceMinor = 1_000,
            mode = ItemSplitMode.EQUAL,
            entries = listOf(entry(PIERRE, 0)),
        )

        // When
        val shares = BillItemsSplit.itemSharesOf(loose, payerIds = listOf(PIERRE))

        // Then
        assertNull(shares)
    }

    @Test
    fun `GIVEN items with service and couvert WHEN breaking them down THEN the service follows what each consumed`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "pizza",
                    quantity = 1,
                    unitPriceMinor = 6_000,
                    mode = ItemSplitMode.AMOUNT,
                    entries = listOf(entry(PIERRE, 4_000), entry(ANA, 2_000)),
                ),
            ),
            servicePercent = 10,
            couvertMinor = 250,
            couvertMemberIds = listOf(PIERRE, ANA, BRUNO),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertEquals(6_000L, breakdown.itemsMinor)
        assertEquals(600L, breakdown.serviceMinor)
        assertEquals(750L, breakdown.couvertMinor)
        assertEquals(7_350L, breakdown.totalMinor)
        assertEquals(
            listOf(share(PIERRE, 4_650, 4_650), share(ANA, 2_450, 2_450), share(BRUNO, 250, 250)),
            breakdown.shares,
        )
    }

    @Test
    fun `GIVEN a service that does not divide evenly WHEN breaking down THEN its leftover cent goes to whoever paid`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "drink",
                    quantity = 1,
                    unitPriceMinor = 1_005,
                    mode = ItemSplitMode.EQUAL,
                    entries = listOf(entry(PIERRE, 1), entry(ANA, 1), entry(BRUNO, 1)),
                ),
            ),
            servicePercent = 10,
            couvertMinor = 0,
            couvertMemberIds = emptyList(),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(BRUNO))

        // Then
        assertEquals(101L, breakdown.serviceMinor)
        assertEquals(listOf(33L, 33L, 35L), breakdown.people.map { it.serviceMinor })
        assertEquals(1_106L, breakdown.shares?.sumOf { it.amountMinor })
    }

    @Test
    fun `GIVEN an item without anyone WHEN breaking down THEN it is loose and there are no shares`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "pizza",
                    entries = listOf(entry(PIERRE, 1)),
                ),
                item(
                    id = "water",
                    entries = emptyList(),
                ),
            ),
            servicePercent = 0,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertEquals(listOf("water"), breakdown.looseItemIds)
        assertNull(breakdown.shares)
    }

    @Test
    fun `GIVEN a difference shared WHEN breaking down THEN a negative one comes off as each consumed`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "pizza",
                    quantity = 1,
                    unitPriceMinor = 3_000,
                    mode = ItemSplitMode.SHARES,
                    entries = listOf(entry(PIERRE, 2), entry(ANA, 1)),
                ),
            ),
            servicePercent = 0,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE, ANA),
            extraMinor = -300,
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertEquals(listOf(-200L, -100L), breakdown.people.map { it.extraMinor })
        assertEquals(2_700L, breakdown.totalMinor)
    }

    @Test
    fun `GIVEN a service but nothing consumed WHEN breaking down THEN the fees have nobody to go to`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "pizza",
                    entries = emptyList(),
                ),
            ),
            servicePercent = 10,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertTrue(breakdown.hasUnassignedFees)
        assertNull(breakdown.shares)
    }

    @Test
    fun `GIVEN the service rule WHEN the percentage gives half a cent THEN it rounds half up`() {
        // Given
        val itemsMinor = 1_005L

        // When
        val service = BillItemsSplit.serviceOf(
            itemsMinor = itemsMinor,
            servicePercent = 10,
        )

        // Then
        assertEquals(101L, service)
    }

    @Test
    fun `GIVEN items the server would refuse WHEN checking their shape THEN they are not well formed`() {
        // Given
        val valid = items(
            items = listOf(
                item(
                    id = "pizza",
                    entries = listOf(entry(PIERRE, 1)),
                ),
            ),
            servicePercent = 10,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE),
        )
        val invalid = listOf(
            valid.copy(items = emptyList()),
            valid.copy(items = valid.items.map { it.copy(name = " ") }),
            valid.copy(items = valid.items.map { it.copy(quantity = 0) }),
            valid.copy(items = valid.items + valid.items),
            valid.copy(servicePercent = 101),
            valid.copy(couvertMinor = -1),
        )

        // When
        val isValid = BillItemsSplit.isWellFormed(valid)
        val areInvalid = invalid.map(BillItemsSplit::isWellFormed)

        // Then
        assertTrue(isValid)
        areInvalid.forEach(::assertFalse)
    }

    private fun item(
        id: String = "item",
        quantity: Long = 1,
        unitPriceMinor: Long = 1_000,
        mode: ItemSplitMode = ItemSplitMode.EQUAL,
        entries: List<SplitEntry>,
    ): BillItem = BillItem(
        id = id,
        name = "Item $id",
        quantity = quantity,
        unitPriceMinor = unitPriceMinor,
        mode = mode,
        entries = entries,
    )

    private fun items(
        items: List<BillItem>,
        servicePercent: Long,
        couvertMinor: Long,
        couvertMemberIds: List<String>,
        extraMinor: Long = 0,
    ): BillItems = BillItems(
        items = items,
        servicePercent = servicePercent,
        couvertMinor = couvertMinor,
        couvertMemberIds = couvertMemberIds,
        extraMinor = extraMinor,
        isFromReceipt = false,
    )

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
    }
}
