package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class InviteGroupOption(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
)
