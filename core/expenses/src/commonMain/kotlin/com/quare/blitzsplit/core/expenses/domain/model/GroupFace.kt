package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor

data class GroupFace(
    val initials: String,
    val color: AvatarColor,
)

fun faceOf(member: GroupMember): GroupFace = GroupFace(
    initials = Initials.firstOf(member.name),
    color = member.avatarColor,
)
