package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

data class Group(
    val id: String,
    val name: String,
    val type: GroupType,
    val color: AvatarColor,
    val currency: GroupCurrency,
    // Why: in the order they were added, so the creator comes first.
    val members: List<GroupMember>,
    // Why: created or changed on this device and not up on the server yet.
    val isPending: Boolean,
    // Why: the code of the invite link, null until the group's first sync brings the one the server made.
    val inviteCode: String?,
)
