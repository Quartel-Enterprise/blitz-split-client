package com.quare.blitzsplit.core.activity.domain.model

/*
 * Why: a change saved on this device and not up yet ("Aguardando envio"), drawn on top of Hoje as the event the server
 * will record for it: a bill created, edited or deleted, a payment recorded or undone, a group created or edited.
 */
data class QueuedChange(
    // Why: the bill's, payment's or group's id, so the item opens it.
    val id: String,
    val groupId: String,
    val kind: ActivityKind,
    val subject: String?,
    val amountMinor: Long?,
    val code: String?,
    val fromMemberId: String?,
    val toMemberId: String?,
    val savedAt: Long,
)
