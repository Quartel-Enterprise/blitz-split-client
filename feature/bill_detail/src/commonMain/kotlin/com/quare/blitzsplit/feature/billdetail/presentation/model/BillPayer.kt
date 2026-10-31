package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: "Bruno pagou" with Bruno's face, "Ana e Bruno pagaram" or "3 pessoas pagaram"; each UI says "Você" for isMe.
data class BillPayer(
    // Why: null for someone who paid and is no longer in the group, whom each UI calls "Ex-membro".
    val name: String?,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
)
