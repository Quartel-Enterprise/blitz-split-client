package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate

data class ExportPayment(
    val id: String,
    val date: LocalDate,
    val paidAt: Long,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val currency: GroupCurrency,
    val method: SettlementMethod,
)
