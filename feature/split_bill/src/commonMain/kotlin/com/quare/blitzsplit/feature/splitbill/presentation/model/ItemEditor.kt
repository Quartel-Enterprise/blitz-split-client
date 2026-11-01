package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: "Item" (7.6), one item of "Itens da conta" being added or changed: its name, how many, the price of each and
 * who consumed it in one of its modes. It changes the items only on "Pronto", which waits until it has a name, a
 * price and someone, and its split closes.
 */
data class ItemEditor(
    // Why: the item being changed, null while adding one.
    val itemId: String?,
    val name: String,
    val quantity: Long,
    // Why: "Preço de cada" as typed, "14,00".
    val unitPriceText: String,
    val mode: ItemSplitMode,
    val rows: List<ItemPersonRow>,
    // Why: who was changed last, next to whom an item that doesn't close says what is missing (7.6f).
    val lastEditedMemberId: String?,
    val payerIds: List<String>,
    val currency: GroupCurrency,
) {
    val unitPriceMinor: Long? = AmountInput.parse(unitPriceText, currency)

    // Why: "Total do item", 0 while the price isn't one.
    val totalMinor: Long = (unitPriceMinor ?: 0L) * quantity

    val entries: List<SplitEntry> = rows.map { row ->
        SplitEntry(
            memberId = row.person.id,
            value = when (mode) {
                ItemSplitMode.EQUAL -> if (row.isIncluded) 1L else 0L
                ItemSplitMode.UNITS, ItemSplitMode.SHARES -> row.count
                ItemSplitMode.AMOUNT -> AmountInput.parse(row.valueText, currency) ?: 0L
                ItemSplitMode.PERCENT -> AmountInput.parseWhole(row.valueText) ?: 0L
            },
        )
    }

    // Why: each one's part of the item, null while it doesn't close or has nobody.
    val shares: List<ExpenseShare>? = BillItemsSplit.itemSharesOf(
        item = toItem(id = itemId.orEmpty()),
        payerIds = payerIds,
    )

    val isNew: Boolean
        get() = itemId == null

    val isNameValid: Boolean
        get() = name.isNotBlank()

    val isPriceValid: Boolean
        get() = unitPriceMinor != null &&
            unitPriceMinor > 0 &&
            ExpenseRules.isAmountValid(totalMinor, currency)

    // Why: "Pronto" only for an item with a name, a price and someone, whose split closes (7.6f, 7.6g).
    val canSave: Boolean
        get() = isNameValid && isPriceValid && shares != null

    // Why: "Unidades só aparece quando o item tem mais de uma."
    val modes: List<ItemSplitMode>
        get() = ItemSplitMode.entries.filter { it != ItemSplitMode.UNITS || quantity > 1 }

    val canDecreaseQuantity: Boolean
        get() = quantity > 1

    val canIncreaseQuantity: Boolean
        get() = quantity < BillItemsSplit.MAX_QUANTITY

    private val included: List<ItemPersonRow>
        get() = entries.zip(rows).filter { (entry, _) -> entry.value > 0 }.map { (_, row) -> row }

    private val markedSum: Long
        get() = entries.sumOf { it.value }

    val status: ItemStatus
        get() {
            if (included.isEmpty()) return ItemStatus.Nobody
            val sum = markedSum
            return when (mode) {
                ItemSplitMode.UNITS -> when {
                    sum < quantity -> ItemStatus.UnitsLeft(sum, quantity)
                    sum > quantity -> ItemStatus.UnitsOver(sum, quantity)
                    else -> ItemStatus.UnitsAddUp(quantity, unitPriceMinor ?: 0L)
                }

                ItemSplitMode.EQUAL -> ItemStatus.Each(
                    eachMinor = totalMinor / included.size,
                    totalMinor = totalMinor,
                    count = included.size,
                )

                ItemSplitMode.SHARES -> ItemStatus.Parts(
                    parts = sum,
                    partMinor = totalMinor / sum,
                    severalParts = included
                        .filter { it.count > 1 }
                        .map {
                            ItemPersonParts(
                                person = it.person,
                                parts = it.count,
                            )
                        },
                    totalMinor = totalMinor,
                    count = included.size,
                )

                ItemSplitMode.AMOUNT -> when {
                    sum < totalMinor -> ItemStatus.AmountLeft(totalMinor - sum, sum, totalMinor)
                    sum > totalMinor -> ItemStatus.AmountOver(sum - totalMinor, sum, totalMinor)
                    else -> ItemStatus.AmountAddsUp(totalMinor)
                }

                ItemSplitMode.PERCENT -> when {
                    sum < BillSplit.FULL_PERCENT -> ItemStatus.PercentLeft(BillSplit.FULL_PERCENT - sum, sum)
                    sum > BillSplit.FULL_PERCENT -> ItemStatus.PercentOver(sum - BillSplit.FULL_PERCENT, sum)
                    else -> ItemStatus.PercentAddsUp(totalMinor, included.size)
                }
            }
        }

    val isStatusError: Boolean
        get() = status.isError

    // Why: the bar under the status by Unidades, Valor and %, how much of the item is marked; null in the other modes.
    val progress: Float?
        get() {
            val sum = markedSum.toFloat()
            val whole = when (mode) {
                ItemSplitMode.UNITS -> quantity
                ItemSplitMode.AMOUNT -> totalMinor
                ItemSplitMode.PERCENT -> BillSplit.FULL_PERCENT
                else -> return null
            }
            if (whole <= 0L || sum <= 0f) return null
            return (sum / whole).coerceAtMost(1f)
        }

    val lines: List<ItemPersonLine>
        get() {
            val byMember = shares?.associate { it.memberId to it.amountMinor }
            val missesNext = isStatusError && status != ItemStatus.Nobody
            return rows.map { row ->
                ItemPersonLine(
                    row = row,
                    shareMinor = byMember?.get(row.person.id),
                    isError = missesNext && row.person.id == lastEditedMemberId,
                )
            }
        }

    // Why: the item as "Pronto" saves it, its name trimmed.
    fun toItem(id: String): BillItem = BillItem(
        id = id,
        name = name.trim(),
        quantity = quantity,
        unitPriceMinor = unitPriceMinor ?: 0L,
        mode = mode,
        entries = entries,
    )
}
