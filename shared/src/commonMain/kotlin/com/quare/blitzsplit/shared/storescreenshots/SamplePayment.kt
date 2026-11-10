package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod

data class SamplePayment(
    val fromPerson: Int,
    val toPerson: Int,
    val amountMinor: Long,
    val method: SettlementMethod,
    val daysAgo: Int,
    val minuteOfDay: Int,
)
