package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a group "Rachar em qual grupo?" lists, with "6 pessoas" under its name.
data class BillGroup(
    val id: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    // Why: drawn in place of the color and the initials, null for none.
    val photoUrl: String?,
    val memberCount: Int,
)
