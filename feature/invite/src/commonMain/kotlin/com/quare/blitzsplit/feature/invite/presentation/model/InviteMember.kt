package com.quare.blitzsplit.feature.invite.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class InviteMember(
    val id: String,
    val name: String,
    // Why: "AN" for Ana; this device's user shows the single initial of the home's avatar.
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: "você", who is already in the group.
    val isMe: Boolean,
    // Why: "Entrou" once someone opened the link and picked this member, "Não entrou" until then.
    val hasJoined: Boolean,
)
