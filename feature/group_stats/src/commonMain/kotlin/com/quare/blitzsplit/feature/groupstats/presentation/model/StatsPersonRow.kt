package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a person of "Por pessoa"; a null name is someone who left the group ("Ex-membro").
data class StatsPersonRow(
    val memberId: String,
    val name: String?,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
    val amountMinor: Long,
    // Why: the bar, scaled by the largest amount of the list.
    val barFraction: Double,
)
