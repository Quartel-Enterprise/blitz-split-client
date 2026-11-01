package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class AddGroupOption(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    // Why: "10 membros".
    val memberCount: Int,
    val isAlreadyIn: Boolean,
)
