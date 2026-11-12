package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: the group's incomes for "By income", as its members saved them on any device, which Dividir's sixth mode starts
 * from. In the group's currency, which the bill's may differ from: only their ratio counts.
 */
data class BillIncomes(
    // Why: by member id in minor units, only the ones above 0.
    val byMemberId: Map<String, Long>,
    val currency: GroupCurrency,
) {
    companion object {
        val None = BillIncomes(emptyMap(), GroupCurrency.BRL)
    }
}
