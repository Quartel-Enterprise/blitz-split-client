package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod

fun interface SettleUp {
    /*
     * Why: "Acertar" or "Recebi" in the global Saldos, one payment per group so each group closes right, all or
     * none; the ids of the payments recorded.
     */
    suspend operator fun invoke(
        legs: List<SettlementLeg>,
        method: SettlementMethod,
        paidAt: Long,
        personName: String?,
        isPaidByMe: Boolean,
    ): Result<List<String>>
}
