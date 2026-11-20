package com.quare.blitzsplit.feature.watchbalances.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: one pair of a group with you in it (15.4): "you owe Carla" or "Ana owes you".
data class WatchDebt(
    val memberId: String,
    // Why: null for someone no longer in the group; each UI writes "Former member".
    val name: String?,
    val initials: String,
    val color: AvatarColor?,
    val photoUrl: String?,
    // Why: positive when they owe you, negative when you owe them.
    val amountMinor: Long,
) {
    val isOwedByMe: Boolean
        get() = amountMinor < 0
}
