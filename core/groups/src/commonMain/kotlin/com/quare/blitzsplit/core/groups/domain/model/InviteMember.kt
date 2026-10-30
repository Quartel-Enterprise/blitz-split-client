package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: hasJoined is whether someone already joined as this member, so it can't be picked anymore.
data class InviteMember(
    val id: String,
    val name: String,
    val avatarColor: AvatarColor,
    val hasJoined: Boolean,
)
