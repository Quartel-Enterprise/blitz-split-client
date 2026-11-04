package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class BillItemTest {
    @Test
    fun `GIVEN six beers WHEN reading the item total THEN it is the quantity times the price of each`() {
        // Given
        val beers = item(
            quantity = 6,
            unitPriceMinor = 1_400,
        )

        // When
        val total = beers.totalMinor

        // Then
        assertEquals(8_400L, total)
    }

    @Test
    fun `GIVEN 999 units at the ether cap WHEN reading the item total THEN it is exact`() {
        // Given
        val ether = item(
            quantity = 999,
            unitPriceMinor = 9_000_000_000_000_000,
        )

        // When
        val total = ether.totalMinor

        // Then
        assertEquals(8_991_000_000_000_000_000L, total)
    }

    @Test
    fun `GIVEN 999 units at a price of 16 digits WHEN reading the item total THEN holds at the largest Long`() {
        // Given
        val typing = item(
            quantity = 999,
            unitPriceMinor = 9_999_999_999_999_999,
        )

        // When
        val total = typing.totalMinor

        // Then
        assertEquals(Long.MAX_VALUE, total)
    }

    private fun item(
        quantity: Long,
        unitPriceMinor: Long,
    ): BillItem = BillItem(
        id = "item",
        name = "Item",
        quantity = quantity,
        unitPriceMinor = unitPriceMinor,
        mode = ItemSplitMode.EQUAL,
        entries = listOf(
            SplitEntry(
                memberId = "pierre",
                value = 1,
            ),
        ),
    )
}
