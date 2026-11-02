package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class MemberFace(
    val initial: String,
    val color: AvatarColor,
    // Why: the profile photo over the initials, null for someone with none or nobody joined as.
    val photoUrl: String?,
)
