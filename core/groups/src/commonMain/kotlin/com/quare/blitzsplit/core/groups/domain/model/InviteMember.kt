package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: hasJoined is whether someone already joined as this member, so it can't be picked anymore.
data class InviteMember(
    val id: String,
    val name: String,
    val avatarColor: AvatarColor,
    val hasJoined: Boolean,
    // Why: the profile photo of whoever joined as this member; null for a name nobody joined as and for no photo.
    val photoUrl: String?,
)
