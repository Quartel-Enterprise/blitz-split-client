package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: one call the tests' RecordSettlement received.
internal data class RecordedCall(
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currency: GroupCurrency,
    val method: SettlementMethod,
    val paidAt: Long,
)
