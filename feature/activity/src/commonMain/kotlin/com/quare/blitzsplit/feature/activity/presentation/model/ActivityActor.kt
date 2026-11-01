package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: who did it, as their member in the group: "Você" for this device's user, whom each UI names.
data class ActivityActor(
    // Why: null when the server kept no name (an account deleted before it had a member).
    val name: String?,
    val isMe: Boolean,
    val initials: String,
    val color: AvatarColor,
)
