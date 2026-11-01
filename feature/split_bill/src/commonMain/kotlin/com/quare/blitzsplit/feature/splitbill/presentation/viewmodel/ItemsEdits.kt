package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillPerson
import com.quare.blitzsplit.feature.splitbill.presentation.model.FeeEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemDeletion
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemPersonRow
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsEditor
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor

// Why: "Serviço (10%)", what a restaurant in Brazil usually adds, which a bill without it sets to 0.
internal const val DEFAULT_SERVICE_PERCENT = 10L

// Why: no item yet, the usual service and no couvert, everyone in the group at the table.
internal fun emptyItemsOf(people: List<BillPerson>): BillItems = BillItems(
    items = emptyList(),
    servicePercent = DEFAULT_SERVICE_PERCENT,
    couvertMinor = 0L,
    couvertMemberIds = people.map { it.id },
    extraMinor = 0L,
    isFromReceipt = false,
)

/*
 * Why: the items among the group's people as they are now. Whoever left takes their marks along, and an item that no
 * longer closes without them splits equally among who is left (with nobody, it shows in red). A new bill's table is
 * everyone in the group; a saved one keeps its own, minus who left.
 */
internal fun BillItems.toItemsAmong(
    people: List<BillPerson>,
    isWholeGroupAtTable: Boolean,
): BillItems {
    val ids = people.map { it.id }.toSet()
    return copy(
        items = items.map { item ->
            val kept = item.entries.filter { it.memberId in ids }
            if (kept.size == item.entries.size) {
                item
            } else {
                item.copy(entries = kept).takeIf { BillItemsSplit.itemSharesOf(it, emptyList()) != null }
                    ?: item.copy(
                        mode = ItemSplitMode.EQUAL,
                        entries = kept.map { it.copy(value = if (it.value > 0) 1L else 0L) },
                    )
            }
        },
        couvertMemberIds = if (isWholeGroupAtTable) people.map { it.id } else couvertMemberIds.filter { it in ids },
    )
}

// Why: the bill's split once "Pronto" closes "Itens da conta".
internal fun ItemsEditor.toSplitEditor(base: SplitEditor): SplitEditor = base
    .copy(
        mode = SplitMode.ITEMS,
        totalMinor = totalMinor,
        payerIds = payerIds,
        items = split,
    ).withShares()

internal fun ItemsEditor.withSplit(change: (BillItems) -> BillItems): ItemsEditor = copy(split = change(split))

// Why: an item "Pronto" saved takes its own place, or comes last once added.
internal fun ItemsEditor.withItem(item: BillItem): ItemsEditor = withSplit { split ->
    val exists = split.items.any { it.id == item.id }
    split.copy(items = if (exists) split.items.map { if (it.id == item.id) item else it } else split.items + item)
}

internal fun ItemsEditor.toItemsWithout(itemId: String): ItemsEditor = withSplit { split ->
    split.copy(items = split.items.filterNot { it.id == itemId })
}

// Why: what "Apagar este item?" says, with what the items fall short of the total once it goes.
internal fun ItemsEditor.deletionOf(itemId: String): ItemDeletion? {
    val item = items.firstOrNull { it.id == itemId } ?: return null
    val left = toItemsWithout(itemId).remainingMinor
    return ItemDeletion(
        itemId = item.id,
        name = item.name,
        totalMinor = item.totalMinor,
        redoesService = split.servicePercent > 0,
        leftMinor = left.takeIf { it > 0 },
    )
}

/*
 * Why: "Item" for an item of the list, as it was typed, or for a new one: one of it, with nobody marked yet, priced at
 * the difference when it comes from "Adicionar item de R$ 15,00".
 */
internal fun itemEditorOf(
    item: BillItem?,
    people: List<BillPerson>,
    payerIds: List<String>,
    currency: GroupCurrency,
    decimalSeparator: Char,
    priceMinor: Long?,
): ItemEditor {
    val values = item?.entries?.associate { it.memberId to it.value }.orEmpty()
    val mode = item?.mode ?: ItemSplitMode.EQUAL
    return ItemEditor(
        itemId = item?.id,
        name = item?.name.orEmpty(),
        quantity = item?.quantity ?: 1L,
        unitPriceText = (item?.unitPriceMinor ?: priceMinor)
            ?.let { AmountInput.format(it, currency, decimalSeparator) }
            .orEmpty(),
        mode = mode,
        rows = people.map { person ->
            val value = values[person.id] ?: 0L
            ItemPersonRow(
                person = person,
                isIncluded = value > 0,
                count = if (mode == ItemSplitMode.UNITS || mode == ItemSplitMode.SHARES) value else 0L,
                valueText = valueTextOf(mode, value, currency, decimalSeparator),
            )
        },
        lastEditedMemberId = null,
        payerIds = payerIds,
        currency = currency,
    )
}

internal fun ItemEditor.withRows(change: (ItemPersonRow) -> ItemPersonRow): ItemEditor = copy(rows = rows.map(change))

/*
 * Why: a new mode starts from who consumed it now: Igual with them, Unidades with the item's units shared as evenly as
 * whole numbers allow, Partes with one each, Valor with equal parts and % with 100 shared as evenly as whole numbers
 * allow. Nobody marked stays nobody.
 */
internal fun ItemEditor.toMode(
    mode: ItemSplitMode,
    decimalSeparator: Char,
): ItemEditor {
    if (mode == this.mode || mode !in modes) return this
    val inItem = entries.filter { it.value > 0 }.map { it.memberId }.toSet()
    val equal = rows.map {
        SplitEntry(
            memberId = it.person.id,
            value = if (it.person.id in inItem) 1L else 0L,
        )
    }
    val values: Map<String, Long> = when (mode) {
        ItemSplitMode.EQUAL, ItemSplitMode.SHARES -> equal.associate { it.memberId to it.value }
        ItemSplitMode.UNITS -> splitEvenlyAmong(quantity, equal, payerIds)
        ItemSplitMode.AMOUNT -> splitEvenlyAmong(totalMinor, equal, payerIds)
        ItemSplitMode.PERCENT -> splitEvenlyAmong(BillSplit.FULL_PERCENT, equal, payerIds)
    }
    return copy(
        mode = mode,
        lastEditedMemberId = null,
    ).withRows { row ->
        val value = values[row.person.id] ?: 0L
        row.copy(
            isIncluded = row.person.id in inItem,
            count = if (mode == ItemSplitMode.UNITS || mode == ItemSplitMode.SHARES) value else row.count,
            valueText = valueTextOf(mode, value, currency, decimalSeparator),
        )
    }
}

/*
 * Why: a new quantity or price. Valor's parts follow the item's new total as a bill's "Valores" follows its own, and
 * one of it no longer splits by units, so it splits equally among who had some.
 */
internal fun ItemEditor.withTotal(
    quantity: Long,
    unitPriceText: String,
    decimalSeparator: Char,
): ItemEditor {
    val changed = copy(
        quantity = quantity,
        unitPriceText = unitPriceText,
    )
    if (changed.mode == ItemSplitMode.UNITS && quantity <= 1) {
        return changed.toMode(ItemSplitMode.EQUAL, decimalSeparator)
    }
    if (mode != ItemSplitMode.AMOUNT || totalMinor <= 0L || changed.totalMinor <= 0L) return changed
    val scaled = BillSplit
        .calculateRescaledAmounts(
            entries = entries,
            oldTotalMinor = totalMinor,
            newTotalMinor = changed.totalMinor,
            payerIds = payerIds,
        ).associate { it.memberId to it.value }
    return changed.withRows { row ->
        row.copy(valueText = valueTextOf(mode, scaled[row.person.id] ?: 0L, currency, decimalSeparator))
    }
}

// Why: the service's percentage or the couvert per person, as the field opens with it; empty while there is none.
internal fun ItemsEditor.feeEditorOf(
    fee: ItemsFee,
    decimalSeparator: Char,
): FeeEditor = FeeEditor(
    fee = fee,
    valueText = when (fee) {
        ItemsFee.SERVICE ->
            split.servicePercent
                .takeIf { it > 0 }
                ?.toString()
                .orEmpty()

        ItemsFee.COUVERT ->
            split.couvertMinor
                .takeIf { it > 0 }
                ?.let { AmountInput.format(it, currency, decimalSeparator) }
                .orEmpty()
    },
    currency = currency,
)

internal fun ItemsEditor.withFee(editor: FeeEditor): ItemsEditor {
    val value = editor.value ?: return this
    return withSplit {
        when (editor.fee) {
            ItemsFee.SERVICE -> it.copy(servicePercent = value)
            ItemsFee.COUVERT -> it.copy(couvertMinor = value)
        }
    }
}

private fun valueTextOf(
    mode: ItemSplitMode,
    value: Long,
    currency: GroupCurrency,
    decimalSeparator: Char,
): String = when {
    value <= 0 -> ""
    mode == ItemSplitMode.AMOUNT -> AmountInput.format(value, currency, decimalSeparator)
    mode == ItemSplitMode.PERCENT -> value.toString()
    else -> ""
}

private fun splitEvenlyAmong(
    total: Long,
    entries: List<SplitEntry>,
    payerIds: List<String>,
): Map<String, Long> = BillSplit
    .split(
        totalMinor = total,
        mode = SplitMode.EQUAL,
        entries = entries,
        payerIds = payerIds,
    )?.associate { it.memberId to it.amountMinor }
    .orEmpty()

/*
 * Why: a sync may bring or take people while "Itens da conta" or "Item" is open: whoever joined comes in without
 * anything marked, and whoever left takes their marks along.
 */
internal fun SplitBillUiState.withItemsAmong(people: List<BillPerson>): SplitBillUiState = copy(
    itemsEditor = itemsEditor?.let {
        it.copy(
            split = it.split.toItemsAmong(people, isWholeGroupAtTable = !isEditing),
            people = people,
            // Why: a payer who left gives way to who pays now, whom the saved shares' cents must follow.
            payerIds = payerIds,
        )
    },
    itemEditor = itemEditor?.let { editor ->
        val byId = editor.rows.associateBy { it.person.id }
        editor.copy(
            payerIds = payerIds,
            rows = people.map { person ->
                byId[person.id]?.copy(person = person) ?: ItemPersonRow(
                    person = person,
                    isIncluded = false,
                    count = 0L,
                    valueText = "",
                )
            },
        )
    },
)
