package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod

// Why: "Ana te pagou · 12:10 · Acerto · Pix · R$ 50,00 · acerto"; tapping it asks "Apagar este acerto?".
data class PaymentRow(
    override val id: String,
    // Why: each UI writes the time of day.
    val paidAt: Long,
    // Why: null for someone no longer in the group ("Ex-membro").
    val fromName: String?,
    // Why: "Você pagou Bruno".
    val fromIsMe: Boolean,
    val toName: String?,
    // Why: "Ana te pagou".
    val toIsMe: Boolean,
    val amountMinor: Long,
    val method: SettlementMethod,
    // Why: "na fila" and the orange dot: saved here and not on the server yet.
    val isPending: Boolean,
) : DayEntry
