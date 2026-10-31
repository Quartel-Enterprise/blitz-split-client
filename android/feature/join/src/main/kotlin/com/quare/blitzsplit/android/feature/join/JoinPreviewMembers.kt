package com.quare.blitzsplit.android.feature.join

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.join.presentation.model.JoinMember

// Why: the previews of the invite and of "Quem é você no grupo?" show the same group.
internal fun createPreviewMembers(): List<JoinMember> = listOf(
    JoinMember("1", "Pierre", "PI", AvatarColor.BLUE, hasJoined = true),
    JoinMember("2", "Ana", "AN", AvatarColor.GREEN, hasJoined = true),
    JoinMember("3", "Bruno", "BR", AvatarColor.PEACH, hasJoined = false),
    JoinMember("4", "Carla", "CA", AvatarColor.LILAC, hasJoined = false),
)
