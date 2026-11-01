package com.quare.blitzsplit.core.expenses.domain.model

// Why: one line of "Itens da conta": what was ordered, how many, the price of each and who consumed it.
data class BillItem(
    val id: String,
    val name: String,
    val quantity: Long,
    val unitPriceMinor: Long,
    val mode: ItemSplitMode,
    /*
     * Why: who consumed it, in the group's order: 1 or 0 for Igual, the units for Unidades, the parts for Partes, the
     * part in the minor unit for Valor and the whole percentage for %. Someone at 0 didn't consume it.
     */
    val entries: List<SplitEntry>,
) {
    val totalMinor: Long
        get() = quantity * unitPriceMinor

    // Why: 7.5c, an item nobody is marked in, which keeps "Pronto" disabled and shows in red.
    val hasNobody: Boolean
        get() = entries.none { it.value > 0 }
}
