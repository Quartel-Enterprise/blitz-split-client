package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a member "Buscar" found, once per group they are in; this device's user never, who is in every group.
data class SearchPersonHit(
    val memberId: String,
    val name: String,
    val nameMatch: TextMatch,
    val initial: String,
    val color: AvatarColor,
    val groupId: String,
    val groupName: String,
)
