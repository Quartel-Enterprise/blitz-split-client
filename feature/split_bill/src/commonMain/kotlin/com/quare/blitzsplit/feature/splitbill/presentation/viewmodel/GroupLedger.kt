package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: what a bill's settled pairs are worked out from, as Room has the group now (7.4b).
internal data class GroupLedger(
    val group: Group?,
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
)
