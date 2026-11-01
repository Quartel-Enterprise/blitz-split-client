package com.quare.blitzsplit.core.expenses.domain.model

// Why: what a bill's items add up to and what each one owes of them, as BillItemsSplit works it out.
data class ItemsBreakdown(
    val itemsMinor: Long,
    val serviceMinor: Long,
    // Why: the couvert of the whole table, the per person value times who is at it.
    val couvertMinor: Long,
    val extraMinor: Long,
    // Why: everyone at the table and whoever consumed something, in the group's order.
    val people: List<ItemsPersonTotal>,
    // Why: the items nobody is marked in (7.5c).
    val looseItemIds: List<String>,
    // Why: the items whose split doesn't add up, the loose ones included.
    val unsettledItemIds: List<String>,
    // Why: the service or the difference has nobody to go to, since nobody consumed anything.
    val hasUnassignedFees: Boolean,
) {
    // Why: "Itens, serviço e couvert somam R$ 627,00".
    val totalMinor: Long
        get() = itemsMinor + serviceMinor + couvertMinor + extraMinor

    /*
     * Why: each one's total as the bill's shares, weighted by the amount as "Valores" is, so a version that doesn't
     * know items reads the same split. Null while an item doesn't add up, a fee has nobody to go to or someone would
     * end below zero.
     */
    val shares: List<ExpenseShare>? = people
        .takeIf { unsettledItemIds.isEmpty() && !hasUnassignedFees && people.none { it.totalMinor < 0 } }
        ?.filter { it.totalMinor > 0 }
        ?.map {
            ExpenseShare(
                memberId = it.memberId,
                amountMinor = it.totalMinor,
                weight = it.totalMinor,
            )
        }?.takeIf { it.isNotEmpty() }
}
