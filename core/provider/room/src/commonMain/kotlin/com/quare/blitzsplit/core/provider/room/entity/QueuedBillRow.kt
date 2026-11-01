package com.quare.blitzsplit.core.provider.room.entity

/*
 * Why: the changes saved on this device and not up yet, which the feed shows on top of Hoje as "Aguardando envio": the
 * server records them once they arrive. hasEvent says whether the server already recorded the bill's creation.
 */
data class QueuedBillRow(
    val id: String,
    val groupId: String,
    val description: String,
    val amountMinor: Long,
    val splitMode: String,
    val isDeleted: Boolean,
    val updatedAt: Long,
    val hasEvent: Boolean,
)
