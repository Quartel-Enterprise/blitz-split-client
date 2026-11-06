package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod

// Why: "Ana te pagou · 12:10 · Acerto · Pix · R$ 50,00 · acerto"; tapping it opens its details, whose "Apagar" asks "Apagar este acerto?".
data class PaymentRow(
    override val id: String,
    // Why: each UI writes the time of day.
    val paidAt: Long,
    // Why: "Hoje" or "Ontem" in the details' "Quando", as a bill's own screen words its day.
    val paidDay: RelativeDay,
    // Why: null for someone no longer in the group ("Ex-membro").
    val fromName: String?,
    // Why: "Você pagou Bruno".
    val fromIsMe: Boolean,
    val toName: String?,
    // Why: "Ana te pagou".
    val toIsMe: Boolean,
    val amountMinor: Long,
    val method: SettlementMethod,
    // Why: "na fila" and the amber dot: saved here and not on the server yet.
    val isPending: Boolean,
) : DayEntry
