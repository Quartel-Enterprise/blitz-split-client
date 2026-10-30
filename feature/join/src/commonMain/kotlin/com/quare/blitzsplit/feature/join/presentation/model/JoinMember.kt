package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class JoinMember(
    val id: String,
    val name: String,
    // Why: "AN" for Ana.
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: "Já entrou": someone opened the link and picked this member, who can't be picked again.
    val hasJoined: Boolean,
)
