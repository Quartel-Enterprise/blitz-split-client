package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class CreateGroupMember(
    // Why: a key for the list and for "Remover", made on the device; the group gets its own ids once created.
    val id: String,
    val name: String,
    // Why: "AN" for Ana; this device's user shows the single initial of the home's avatar.
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: "você", and the one member that can't be removed.
    val isMe: Boolean,
)
