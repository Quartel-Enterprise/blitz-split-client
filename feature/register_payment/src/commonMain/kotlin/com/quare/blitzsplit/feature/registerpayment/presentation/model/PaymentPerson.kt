package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: who paid or who received; each UI says "Você" for isMe.
data class PaymentPerson(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
)
