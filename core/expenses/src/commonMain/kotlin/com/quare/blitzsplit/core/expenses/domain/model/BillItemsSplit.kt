package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the math of "Itens da conta", the same on every platform and the one that checks a stored bill. Each item
 * splits among who consumed it the way BillSplit splits a bill, its leftover cents going to whoever paid while they
 * consumed it; the service is a share of the items in proportion to what each one consumed, the couvert the same
 * for everyone at the table, and "Dividir a diferença" in proportion to what each one consumed too.
 */
object BillItemsSplit {
    const val MAX_ITEMS = 200
    const val MAX_QUANTITY = 999L
    const val NAME_MAX_LENGTH = 60
    const val MAX_SERVICE_PERCENT = 100L
    private const val HALF = 50L
    private const val FULL = 100L

    // Why: each one's part of one item, or null while it doesn't add up or has nobody in it.
    fun itemSharesOf(
        item: BillItem,
        payerIds: List<String>,
    ): List<ExpenseShare>? {
        if (item.quantity !in 1..MAX_QUANTITY || item.unitPriceMinor <= 0) return null
        val total = item.totalMinor
        return when (item.mode) {
            ItemSplitMode.EQUAL -> BillSplit.split(total, SplitMode.EQUAL, item.entries, payerIds)
            ItemSplitMode.SHARES -> BillSplit.split(total, SplitMode.SHARES, item.entries, payerIds)
            ItemSplitMode.AMOUNT -> BillSplit.split(total, SplitMode.AMOUNT, item.entries, payerIds)
            ItemSplitMode.PERCENT -> BillSplit.split(total, SplitMode.PERCENT, item.entries, payerIds)
            ItemSplitMode.UNITS -> unitSharesOf(item)
        }
    }

    // Why: "R$ 57,00 de serviço" on R$ 570,00 of items at 10%, rounded half up to the minor unit.
    fun serviceOf(
        itemsMinor: Long,
        servicePercent: Long,
    ): Long = if (servicePercent <= 0 || itemsMinor <= 0) 0L else (itemsMinor * servicePercent + HALF) / FULL

    fun breakdownOf(
        split: BillItems,
        payerIds: List<String>,
    ): ItemsBreakdown {
        val consumers = split.items.flatMap { item -> item.entries.filter { it.value > 0 }.map { it.memberId } }
        val memberIds = (split.couvertMemberIds + consumers).distinct()
        val consumed = mutableMapOf<String, Long>()
        val unsettled = mutableListOf<String>()
        split.items.forEach { item ->
            val shares = itemSharesOf(item, payerIds)
            if (shares == null) {
                unsettled += item.id
            } else {
                shares.forEach { consumed[it.memberId] = (consumed[it.memberId] ?: 0L) + it.amountMinor }
            }
        }
        val itemsMinor = split.items.sumOf { it.totalMinor }
        val serviceMinor = serviceOf(itemsMinor, split.servicePercent)
        val weights = memberIds.map {
            SplitEntry(
                memberId = it,
                value = consumed[it] ?: 0L,
            )
        }
        val service = splitProportionally(serviceMinor, weights, payerIds)
        val extra = splitProportionally(split.extraMinor, weights, payerIds)
        val atTable = split.couvertMemberIds.toSet()
        val people = memberIds.map { memberId ->
            ItemsPersonTotal(
                memberId = memberId,
                itemsMinor = consumed[memberId] ?: 0L,
                serviceMinor = service?.get(memberId) ?: 0L,
                couvertMinor = if (memberId in atTable) split.couvertMinor else 0L,
                extraMinor = extra?.get(memberId) ?: 0L,
            )
        }
        return ItemsBreakdown(
            itemsMinor = itemsMinor,
            serviceMinor = serviceMinor,
            couvertMinor = split.couvertMinor * atTable.size,
            extraMinor = split.extraMinor,
            people = people,
            looseItemIds = split.items.filter { it.hasNobody }.map { it.id },
            unsettledItemIds = unsettled,
            hasUnassignedFees = service == null || extra == null,
        )
    }

    // Why: the bill's shares from its items, null while they don't add up.
    fun sharesOf(
        split: BillItems,
        payerIds: List<String>,
    ): List<ExpenseShare>? = breakdownOf(split, payerIds).shares

    // Why: what the API's check keeps of the items, so a bill it would refuse never waits in the queue for good.
    fun isWellFormed(split: BillItems): Boolean = split.items.size in 1..MAX_ITEMS &&
        split.items.distinctBy { it.id }.size == split.items.size &&
        split.items.all { item ->
            item.name.isNotBlank() &&
                item.name.length <= NAME_MAX_LENGTH &&
                item.quantity in 1..MAX_QUANTITY &&
                item.unitPriceMinor > 0 &&
                item.entries.distinctBy { it.memberId }.size == item.entries.size &&
                item.entries.all { it.value >= 0 }
        } &&
        split.servicePercent in 0..MAX_SERVICE_PERCENT &&
        split.couvertMinor >= 0

    private fun unitSharesOf(item: BillItem): List<ExpenseShare>? {
        val included = item.entries.filter { it.value > 0 }
        if (included.isEmpty() || item.entries.any { it.value < 0 }) return null
        if (included.sumOf { it.value } != item.quantity) return null
        return included.map {
            ExpenseShare(
                memberId = it.memberId,
                amountMinor = it.value * item.unitPriceMinor,
                weight = it.value,
            )
        }
    }

    /*
     * Why: an amount in proportion to what each one consumed, the cents left over to whoever paid as BillSplit does;
     * a negative one (a difference over the total) the same way, taken off. Null while it has nobody to go to.
     */
    private fun splitProportionally(
        amountMinor: Long,
        weights: List<SplitEntry>,
        payerIds: List<String>,
    ): Map<String, Long>? {
        if (amountMinor == 0L) return emptyMap()
        val sign = if (amountMinor < 0) -1L else 1L
        return BillSplit
            .split(
                totalMinor = amountMinor * sign,
                mode = SplitMode.SHARES,
                entries = weights,
                payerIds = payerIds,
            )?.associate { it.memberId to it.amountMinor * sign }
    }
}
