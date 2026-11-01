package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ItemsBreakdown
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Itens da conta" (7.5), Dividir's fifth mode: the items, the service and the couvert, and what each one owes of
 * them. It changes the bill's split only on "Pronto", which waits until every item has someone and the items, service
 * and couvert close with the total.
 */
data class ItemsEditor(
    val split: BillItems,
    // Why: the group's people, in its order, who "Quanto fica para cada um" lists.
    val people: List<BillPerson>,
    // Why: the bill's amount, 0 while the field holds no valid amount.
    val totalMinor: Long,
    // Why: who paid, who keep the cents an item can't share evenly while they consumed it.
    val payerIds: List<String>,
    val currency: GroupCurrency,
) {
    val breakdown: ItemsBreakdown = BillItemsSplit.breakdownOf(split, payerIds)

    val items: List<BillItem>
        get() = split.items

    val rows: List<ItemsItemRow> = split.items.map { item ->
        val consumerIds = item.entries
            .filter { it.value > 0 }
            .map { it.memberId }
            .toSet()
        ItemsItemRow(
            item = item,
            consumers = people.filter { it.id in consumerIds },
            isLoose = item.id in breakdown.unsettledItemIds,
        )
    }

    val personRows: List<ItemsPersonRow> = people.map { person ->
        val total = breakdown.people.firstOrNull { it.memberId == person.id }
        ItemsPersonRow(
            person = person,
            itemsMinor = total?.itemsMinor ?: 0L,
            feesMinor = total?.feesMinor ?: 0L,
            totalMinor = total?.totalMinor ?: 0L,
        )
    }

    // Why: "Faltam R$ 15,00" while positive, "Passou" while negative.
    val remainingMinor: Long
        get() = totalMinor - breakdown.totalMinor

    val looseItems: List<BillItem>
        get() = rows.filter { it.isLoose }.map { it.item }

    val status: ItemsStatus
        get() {
            val loose = looseItems
            return when {
                loose.isNotEmpty() -> ItemsStatus.Loose(
                    items = loose,
                    isEveryItem = loose.size == items.size,
                    looseMinor = loose.sumOf { it.totalMinor },
                )

                remainingMinor > 0 -> ItemsStatus.AmountLeft(
                    amountMinor = remainingMinor,
                    sumMinor = breakdown.totalMinor,
                    totalMinor = totalMinor,
                    isFromReceipt = split.isFromReceipt,
                )

                remainingMinor < 0 -> ItemsStatus.AmountOver(
                    amountMinor = -remainingMinor,
                    sumMinor = breakdown.totalMinor,
                    totalMinor = totalMinor,
                    isFromReceipt = split.isFromReceipt,
                )

                else -> ItemsStatus.AddsUp(
                    totalMinor = totalMinor,
                    itemsMinor = breakdown.itemsMinor,
                    serviceMinor = breakdown.serviceMinor,
                    couvertMinor = breakdown.couvertMinor,
                    extraMinor = breakdown.extraMinor,
                )
            }
        }

    /*
     * Why: "Pronto" only once there is an item, every item has someone and it all closes with the total; a couvert
     * alone may close it, but a bill without items is one the save (BillItemsSplit.isWellFormed) refuses.
     */
    val addsUp: Boolean
        get() = items.isNotEmpty() && totalMinor > 0 && remainingMinor == 0L && breakdown.shares != null

    // Why: "Adicionar item de R$ 15,00", while the items fall short of the total.
    val canAddDifferenceItem: Boolean
        get() = remainingMinor > 0 && items.size < BillItemsSplit.MAX_ITEMS

    /*
     * Why: "Dividir a diferença" shares it in proportion to what each one consumed, so only once every item has
     * someone and, for a total below the items, while the shares it gives (rounding included) leave nobody below zero.
     */
    val canSplitDifference: Boolean
        get() = remainingMinor != 0L &&
            totalMinor > 0 &&
            looseItems.isEmpty() &&
            breakdown.itemsMinor > 0 &&
            BillItemsSplit.breakdownOf(split.copy(extraMinor = split.extraMinor + remainingMinor), payerIds).shares !=
            null

    val canAddItem: Boolean
        get() = items.size < BillItemsSplit.MAX_ITEMS

    // Why: the third row of "Serviço e couvert" once "Dividir a diferença" shared some, which can be taken off.
    val showsExtra: Boolean
        get() = split.extraMinor != 0L
}
