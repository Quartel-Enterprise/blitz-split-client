package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a person of "Acertos", always someone still in the group.
data class StatsSettler(
    val memberId: String,
    val name: String,
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: the profile photo over the initials, null for someone with none or nobody joined as.
    val photoUrl: String?,
    val isMe: Boolean,
    // Why: "em média 2 dias", weighted by each debt's amount; 0 is "no mesmo dia".
    val averageDays: Int,
    // Why: "R$ 80 em aberto", what the period's bills put on them that nothing covered yet; 0 shows nothing.
    val openMinor: Long,
)
