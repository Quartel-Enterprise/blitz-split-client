package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.FeeEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemPersonParts
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemPersonRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: what "Item" (7.6) and "Itens da conta" (7.5) say about themselves in each mode.
class ItemEditorTest {
    @Test
    fun `GIVEN an item split equally WHEN reading its status THEN says what each one pays among how many`() {
        // Given
        val editor = editor(ItemSplitMode.EQUAL, "178,00", included = listOf(PIERRE, ANA))

        // When
        val status = editor.status

        // Then
        assertEquals(
            ItemStatus.Each(
                eachMinor = 8_900L,
                totalMinor = 17_800L,
                count = 2,
            ),
            status,
        )
        assertFalse(editor.isStatusError)
        assertNull(editor.progress)
    }

    @Test
    fun `GIVEN an item by shares WHEN Bruno has two THEN names him and the value of a share`() {
        // Given
        val editor = editor(ItemSplitMode.SHARES, "168,00", counts = mapOf(PIERRE to 1L, BRUNO to 2L))

        // When
        val status = editor.status

        // Then
        assertEquals(
            ItemStatus.Parts(
                parts = 3L,
                partMinor = 5_600L,
                severalParts = listOf(ItemPersonParts(people[2], 2L)),
                totalMinor = 16_800L,
                count = 2,
            ),
            status,
        )
    }

    @Test
    fun `GIVEN an item by value WHEN the parts fall short or go over or close THEN says so with the bar`() {
        // Given
        val short = editor(ItemSplitMode.AMOUNT, "60,00", values = mapOf(PIERRE to "40,00"))
        val over = editor(ItemSplitMode.AMOUNT, "60,00", values = mapOf(PIERRE to "40,00", ANA to "30,00"))
        val closed = editor(ItemSplitMode.AMOUNT, "60,00", values = mapOf(PIERRE to "40,00", ANA to "20,00"))

        // When
        val statuses = listOf(short, over, closed).map { it.status }

        // Then
        assertEquals(
            listOf(
                ItemStatus.AmountLeft(2_000L, 4_000L, 6_000L),
                ItemStatus.AmountOver(1_000L, 7_000L, 6_000L),
                ItemStatus.AmountAddsUp(6_000L),
            ),
            statuses,
        )
        assertEquals(4_000f / 6_000f, short.progress)
        assertTrue(closed.canSave)
        assertFalse(short.canSave)
    }

    @Test
    fun `GIVEN an item by percentage WHEN the percentages fall short or go over or close THEN says so`() {
        // Given
        val short = editor(ItemSplitMode.PERCENT, "60,00", values = mapOf(PIERRE to "67", ANA to "23"))
        val over = editor(ItemSplitMode.PERCENT, "60,00", values = mapOf(PIERRE to "67", ANA to "43"))
        val closed = editor(ItemSplitMode.PERCENT, "60,00", values = mapOf(PIERRE to "67", ANA to "33"))

        // When
        val statuses = listOf(short, over, closed).map { it.status }

        // Then
        assertEquals(
            listOf(
                ItemStatus.PercentLeft(10L, 90L),
                ItemStatus.PercentOver(10L, 110L),
                ItemStatus.PercentAddsUp(6_000L, 2),
            ),
            statuses,
        )
    }

    @Test
    fun `GIVEN units over the quantity WHEN reading the status THEN says how many are over`() {
        // Given
        val editor = editor(
            ItemSplitMode.UNITS,
            "14,00",
            quantity = 2,
            counts = mapOf(PIERRE to 2L, ANA to 1L),
        )

        // When
        val status = editor.status

        // Then
        assertEquals(
            ItemStatus.UnitsOver(
                marked = 3L,
                quantity = 2L,
            ),
            status,
        )
        assertTrue(editor.isStatusError)
    }

    @Test
    fun `GIVEN an item without a name or a price WHEN checking it THEN it cannot be saved`() {
        // Given
        val unnamed = editor(ItemSplitMode.EQUAL, "10,00", included = listOf(PIERRE)).copy(name = " ")
        val unpriced = editor(ItemSplitMode.EQUAL, "", included = listOf(PIERRE))

        // When
        val canSave = listOf(unnamed, unpriced).map { it.canSave }

        // Then
        assertEquals(listOf(false, false), canSave)
    }

    @Test
    fun `GIVEN items over the total WHEN reading the status THEN says how much it went over`() {
        // Given
        val items = itemsEditor(totalMinor = 900L)

        // When
        val status = items.status

        // Then
        assertEquals(
            ItemsStatus.AmountOver(
                amountMinor = 100L,
                sumMinor = 1_000L,
                totalMinor = 900L,
                isFromReceipt = false,
            ),
            status,
        )
        assertTrue(items.canSplitDifference)
        assertFalse(items.canAddDifferenceItem)
    }

    @Test
    fun `GIVEN items nobody has WHEN reading the status THEN lists them with what is left out`() {
        // Given
        val items = itemsEditor(
            totalMinor = 1_000L,
            isLoose = true,
        )

        // When
        val status = items.status

        // Then
        assertTrue(status is ItemsStatus.Loose && status.isEveryItem && status.looseMinor == 1_000L)
        assertFalse(items.addsUp)
        assertFalse(items.canSplitDifference)
    }

    @Test
    fun `GIVEN no item and a couvert that matches the total WHEN reading it THEN Pronto stays disabled`() {
        // Given
        val base = itemsEditor(totalMinor = 750L)
        val items = base.copy(
            split = base.split.copy(
                items = emptyList(),
                couvertMinor = 250L,
            ),
        )

        // When
        val addsUp = items.addsUp

        // Then
        assertEquals(0L, items.remainingMinor)
        assertFalse(addsUp)
    }

    @Test
    fun `GIVEN the service field WHEN typing over 100 or a couvert past the limit THEN it cannot be saved`() {
        // Given
        val service = FeeEditor(ItemsFee.SERVICE, "101", GroupCurrency.BRL, false)
        val couvert = FeeEditor(ItemsFee.COUVERT, "100000001", GroupCurrency.BRL, false)
        val empty = FeeEditor(ItemsFee.SERVICE, "", GroupCurrency.BRL, false)

        // When
        val canSave = listOf(service, couvert, empty).map { it.canSave }

        // Then
        assertEquals(listOf(false, false, true), canSave)
        assertEquals(0L, empty.value)
    }

    @Test
    fun `GIVEN an item of a bitcoin bill in sats WHEN reading its price THEN it is whole sats`() {
        // Given
        val editor = editor(
            ItemSplitMode.EQUAL,
            "1.000",
            quantity = 2L,
            currency = GroupCurrency.BTC,
            showsSats = true,
        )

        // When
        val price = editor.unitPriceMinor

        // Then
        assertEquals(1_000L, price)
        assertEquals(2_000L, editor.totalMinor)
    }

    @Test
    fun `GIVEN an item of a bitcoin bill not in sats WHEN reading its price THEN it has 8 decimals`() {
        // Given
        val editor = editor(ItemSplitMode.EQUAL, "0,00001", currency = GroupCurrency.BTC)

        // When
        val price = editor.unitPriceMinor

        // Then
        assertEquals(1_000L, price)
    }

    @Test
    fun `GIVEN an item by value in sats WHEN the parts close THEN reads each part as whole sats`() {
        // Given
        val editor = editor(
            ItemSplitMode.AMOUNT,
            "1.000",
            values = mapOf(PIERRE to "600", ANA to "400"),
            currency = GroupCurrency.BTC,
            showsSats = true,
        )

        // When
        val status = editor.status

        // Then
        assertEquals(ItemStatus.AmountAddsUp(1_000L), status)
    }

    @Test
    fun `GIVEN a couvert in bitcoin WHEN typing sats or 8 decimals THEN both read the same sats`() {
        // Given
        val inSats = FeeEditor(ItemsFee.COUVERT, "500", GroupCurrency.BTC, showsSats = true)
        val inBitcoin = FeeEditor(ItemsFee.COUVERT, "0,000005", GroupCurrency.BTC, showsSats = false)

        // When
        val values = listOf(inSats.value, inBitcoin.value)

        // Then
        assertEquals(listOf(500L, 500L), values)
    }

    @Test
    fun `GIVEN a couvert in sats WHEN typing over 21 million bitcoin THEN it cannot be saved`() {
        // Given
        val limit = FeeEditor(ItemsFee.COUVERT, "2100000000000000", GroupCurrency.BTC, showsSats = true)
        val over = FeeEditor(ItemsFee.COUVERT, "2100000000000001", GroupCurrency.BTC, showsSats = true)

        // When
        val canSave = listOf(limit, over).map { it.canSave }

        // Then
        assertEquals(listOf(true, false), canSave)
    }

    @Test
    fun `GIVEN a couvert in ether WHEN typing over 90 million ether THEN it cannot be saved`() {
        // Given
        val limit = FeeEditor(ItemsFee.COUVERT, "90000000,00000000", GroupCurrency.ETH, showsSats = false)
        val over = FeeEditor(ItemsFee.COUVERT, "90000000,00000001", GroupCurrency.ETH, showsSats = false)

        // When
        val canSave = listOf(limit, over).map { it.canSave }

        // Then
        assertEquals(listOf(true, false), canSave)
    }

    private val people = listOf(
        person(PIERRE, "Pierre", isMe = true),
        person(ANA, "Ana", isMe = false),
        person(BRUNO, "Bruno", isMe = false),
    )

    private fun editor(
        mode: ItemSplitMode,
        price: String,
        quantity: Long = 1L,
        included: List<String> = emptyList(),
        counts: Map<String, Long> = emptyMap(),
        values: Map<String, String> = emptyMap(),
        currency: GroupCurrency = GroupCurrency.BRL,
        showsSats: Boolean = false,
    ): ItemEditor = ItemEditor(
        itemId = "item",
        name = "Pizza",
        quantity = quantity,
        unitPriceText = price,
        mode = mode,
        rows = people.map {
            ItemPersonRow(
                person = it,
                isIncluded = it.id in included,
                count = counts[it.id] ?: 0L,
                valueText = values[it.id].orEmpty(),
            )
        },
        lastEditedMemberId = null,
        payerIds = listOf(PIERRE),
        currency = currency,
        showsSats = showsSats,
    )

    private fun itemsEditor(
        totalMinor: Long,
        isLoose: Boolean = false,
    ): ItemsEditor = ItemsEditor(
        split = BillItems(
            items = listOf(
                BillItem(
                    id = "pizza",
                    name = "Pizza",
                    quantity = 1L,
                    unitPriceMinor = 1_000L,
                    mode = ItemSplitMode.EQUAL,
                    entries = if (isLoose) emptyList() else listOf(SplitEntry(PIERRE, 1L)),
                ),
            ),
            servicePercent = 0L,
            couvertMinor = 0L,
            couvertMemberIds = people.map { it.id },
            extraMinor = 0L,
            isFromReceipt = false,
        ),
        people = people,
        totalMinor = totalMinor,
        payerIds = listOf(PIERRE),
        currency = GroupCurrency.BRL,
        showsSats = false,
    )

    private fun person(
        id: String,
        name: String,
        isMe: Boolean,
    ): BillPerson = BillPerson(
        id = id,
        name = name,
        initials = name.take(2).uppercase(),
        avatarColor = AvatarColor.BLUE,
        photoUrl = null,
        isMe = isMe,
        hasJoined = true,
    )

    private companion object {
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
    }
}
