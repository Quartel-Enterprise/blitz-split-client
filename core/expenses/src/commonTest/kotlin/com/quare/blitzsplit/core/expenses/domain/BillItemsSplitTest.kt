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
    fun `GIVEN 570 reais of items at 10 percent WHEN working out the service THEN it is 57 reais`() {
        // Given
        val itemsMinor = 57_000L

        // When
        val service = BillItemsSplit.serviceOf(
            itemsMinor = itemsMinor,
            servicePercent = 10,
        )

        // Then
        assertEquals(5_700L, service)
    }

    @Test
    fun `GIVEN the largest items whose percentage fits a Long WHEN working out the service THEN it rounds half up`() {
        // Given
        val itemsMinor = 922_337_203_685_477_575L

        // When
        val service = BillItemsSplit.serviceOf(
            itemsMinor = itemsMinor,
            servicePercent = 10,
        )

        // Then
        assertEquals(92_233_720_368_547_758L, service)
    }

    @Test
    fun `GIVEN items at the largest Long WHEN working out 10 percent of service THEN it is about a tenth`() {
        // Given
        val itemsMinor = Long.MAX_VALUE

        // When
        val service = BillItemsSplit.serviceOf(
            itemsMinor = itemsMinor,
            servicePercent = 10,
        )

        // Then
        val tenth = itemsMinor / 10
        assertTrue(service in tenth - 10..tenth, "service $service is not about $tenth")
    }

    @Test
    fun `GIVEN items held at the largest Long WHEN working out 100 percent of service THEN it is about the items`() {
        // Given
        val itemsMinor = Long.MAX_VALUE

        // When
        val service = BillItemsSplit.serviceOf(
            itemsMinor = itemsMinor,
            servicePercent = 100,
        )

        // Then
        assertTrue(service in itemsMinor - 100..itemsMinor, "service $service is not about $itemsMinor")
    }

    @Test
    fun `GIVEN items at the largest Long WHEN working out 1 percent of service THEN it is about a hundredth`() {
        // Given
        val itemsMinor = Long.MAX_VALUE

        // When
        val service = BillItemsSplit.serviceOf(
            itemsMinor = itemsMinor,
            servicePercent = 1,
        )

        // Then
        val hundredth = itemsMinor / 100
        assertTrue(service in hundredth - 1..hundredth, "service $service is not about $hundredth")
    }

    @Test
    fun `GIVEN two items of 999 at the ether cap WHEN breaking down THEN the items hold at the largest Long`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "first",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
                item(
                    id = "second",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(ANA, 1)),
                ),
            ),
            servicePercent = 0,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE, ANA),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertEquals(Long.MAX_VALUE, breakdown.itemsMinor)
    }

    @Test
    fun `GIVEN one person with two items of 999 at the ether cap WHEN breaking down THEN theirs never wrap`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "first",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
                item(
                    id = "second",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
            ),
            servicePercent = 0,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertEquals(listOf(Long.MAX_VALUE), breakdown.people.map { it.itemsMinor })
    }

    @Test
    fun `GIVEN a couvert of 16 digits for a thousand people WHEN breaking down THEN it holds at the largest Long`() {
        // Given
        val table = (1..1_000).map { "member$it" }
        val split = items(
            items = emptyList(),
            servicePercent = 0,
            couvertMinor = 9_999_999_999_999_999,
            couvertMemberIds = table,
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(table.first()))

        // Then
        assertEquals(Long.MAX_VALUE, breakdown.couvertMinor)
    }

    @Test
    fun `GIVEN items past a Long with 10 percent of service WHEN breaking down THEN the service is positive`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "first",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
                item(
                    id = "second",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
            ),
            servicePercent = 10,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertTrue(breakdown.serviceMinor > 0, "service ${breakdown.serviceMinor} is not positive")
    }

    @Test
    fun `GIVEN items past a Long with 10 percent of service WHEN breaking down THEN the total stays over any bill`() {
        // Given
        val split = items(
            items = listOf(
                item(
                    id = "first",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
                item(
                    id = "second",
                    quantity = 999,
                    unitPriceMinor = ETHER_CAP,
                    entries = listOf(entry(PIERRE, 1)),
                ),
            ),
            servicePercent = 10,
            couvertMinor = 0,
            couvertMemberIds = listOf(PIERRE),
        )

        // When
        val breakdown = BillItemsSplit.breakdownOf(split, payerIds = listOf(PIERRE))

        // Then
        assertTrue(breakdown.totalMinor > ETHER_CAP, "total ${breakdown.totalMinor} is under the ether cap")
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

        // Why: 90 million ether in its minor unit, the largest price an item may have.
        const val ETHER_CAP = 9_000_000_000_000_000L
    }
}
