package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: "Bruno pagou" with Bruno's face; each UI says "Você pagou" for isMe.
data class BillPayer(
    val name: String,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
)
