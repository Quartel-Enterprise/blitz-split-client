package com.quare.blitzsplit.feature.groupdetail.presentation.model

// Why: a pair in Saldos, already netted: "Você deve a Bruno", "Ana te deve" or "Diego deve a Bruno".
data class DebtRow(
    val fromMemberId: String,
    val toMemberId: String,
    // Why: null for someone no longer in the group ("Ex-membro"), whose face is a blank avatar.
    val fromName: String?,
    val fromFace: GroupFace?,
    val toName: String?,
    val toFace: GroupFace?,
    val amountMinor: Long,
    val kind: DebtKind,
)
