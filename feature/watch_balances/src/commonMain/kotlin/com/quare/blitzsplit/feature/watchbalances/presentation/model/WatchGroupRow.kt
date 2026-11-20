package com.quare.blitzsplit.feature.watchbalances.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: one open group in one currency on the watch's Balances (15.3).
data class WatchGroupRow(
    val groupId: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    val photoUrl: String?,
    val currency: GroupCurrency,
    // Why: your net in the group: positive "to receive", negative "you owe", zero when its open pairs offset each other.
    val netMinor: Long,
    // Why: a payment recorded here waits to go up: the orange dot and "waiting to send" (15.6b).
    val isQueued: Boolean,
)
