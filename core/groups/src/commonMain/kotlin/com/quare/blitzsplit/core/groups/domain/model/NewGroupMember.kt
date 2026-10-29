package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

data class NewGroupMember(
    val name: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
)
