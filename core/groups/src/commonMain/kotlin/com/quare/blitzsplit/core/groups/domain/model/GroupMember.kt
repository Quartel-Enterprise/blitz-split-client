package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

data class GroupMember(
    val id: String,
    val name: String,
    val avatarColor: AvatarColor,
    // Why: this device's user, also before the first sync gives it an account.
    val isMe: Boolean,
    // Why: someone who opened the invite link and picked this member; the others are still just a name.
    val hasJoined: Boolean,
)
