package com.quare.blitzsplit.core.activity.domain.model

// Why: "Cobrar" sent in one group: whom (their member) and how much, which the feed shows as "Você cobrou Ana".
data class ChargeSent(
    val groupId: String,
    val memberId: String,
    val memberName: String,
    val amountMinor: Long,
)
