package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class MembersMember(
    val id: String,
    val name: String,
    // Why: "AN" for Ana; this device's user shows the single initial of the home's avatar.
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: "(você)" and "Este aparelho"; this device's user can only be renamed.
    val isMe: Boolean,
    // Why: "Entrou" or "Ainda não entrou". Someone who joined can't be removed: "Ana já entrou no grupo".
    val hasJoined: Boolean,
) {
    val canBeRemoved: Boolean
        get() = !isMe && !hasJoined

    // Why: "Convite pessoal para Carla" is only for someone nobody joined as.
    val canBeInvited: Boolean
        get() = !isMe && !hasJoined
}
