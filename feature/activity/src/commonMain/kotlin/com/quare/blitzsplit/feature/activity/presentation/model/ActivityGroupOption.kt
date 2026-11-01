package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupType

// Why: a group "Filtrar" offers, with the shape and color it has on Grupos.
data class ActivityGroupOption(
    val id: String,
    val name: String,
    val color: AvatarColor,
    val type: GroupType,
)
