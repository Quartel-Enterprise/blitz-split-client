package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod

fun interface RecordSettlement {
    // Why: the new payment's id.
    suspend operator fun invoke(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        method: SettlementMethod,
        paidAt: Long,
    ): Result<String>
}
