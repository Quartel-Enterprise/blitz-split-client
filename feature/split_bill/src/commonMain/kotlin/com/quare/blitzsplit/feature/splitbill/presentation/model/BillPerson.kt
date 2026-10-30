package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a member of the group, as "Quem pagou" and "Dividir" list them; each UI says "Você" for isMe.
data class BillPerson(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
    // Why: "Ainda não entrou" under someone nobody joined as.
    val hasJoined: Boolean,
)
