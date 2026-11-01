package com.quare.blitzsplit.core.provider.room.entity

// Why: a payment recorded or undone here and not up yet, "Aguardando envio" in the feed.
data class QueuedPaymentRow(
    val id: String,
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    val method: String,
    val isDeleted: Boolean,
    val updatedAt: Long,
)
