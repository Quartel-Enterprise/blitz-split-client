package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupType

// Why: a group "Filtrar" offers, drawn as its avatar on Grupos: its photo, else its initials on its color.
data class ActivityGroupOption(
    val id: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    // Why: null for a group with no photo.
    val photoUrl: String?,
    val type: GroupType,
)
