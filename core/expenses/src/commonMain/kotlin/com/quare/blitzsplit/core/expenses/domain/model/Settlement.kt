package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a payment made outside the app that settles part or all of what one member owes another.
data class Settlement(
    val id: String,
    val groupId: String,
    // Why: who paid and who received.
    val fromMemberId: String,
    val toMemberId: String,
    // Why: in the minor unit of its currency.
    val amountMinor: Long,
    // Why: the currency of the balance it settles; a payment never mixes two.
    val currency: GroupCurrency,
    val method: SettlementMethod,
    // Why: "Quando", in epoch millis.
    val paidAt: Long,
    // Why: recorded or undone on this device and not up on the server yet ("na fila").
    val isPending: Boolean,
    // Why: when it was recorded or last changed, here or on another device: the group's last activity on the home.
    val updatedAt: Long,
)
