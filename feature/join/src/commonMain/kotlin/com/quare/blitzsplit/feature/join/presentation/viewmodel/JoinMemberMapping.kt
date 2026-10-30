package com.quare.blitzsplit.feature.join.presentation.viewmodel

import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember

internal fun InviteMember.toJoinMember(): JoinMember = JoinMember(
    id = id,
    name = name,
    initials = Initials.of(name),
    avatarColor = avatarColor,
    hasJoined = hasJoined,
)
