package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: one person under the split, "Bruno · Pagou R$ 642,00 · R$ 146,00"; each UI says "Você" for isMe.
data class BillShareRow(
    val memberId: String,
    // Why: null for someone in the bill who is no longer in the group, whom each UI calls "Ex-membro".
    val name: String?,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
    // Why: "Ainda não entrou".
    val hasJoined: Boolean,
    // Why: "Pagou R$ 642,00", null for someone who didn't pay.
    val paidMinor: Long?,
    // Why: their part, null for someone in the group left out of the split ("Não participa").
    val shareMinor: Long?,
)
