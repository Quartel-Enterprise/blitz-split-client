package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a group "All expenses"' Group filter offers, with its rounded square, as Home's rows draw it.
data class FilterGroupOption(
    val id: String,
    val name: String,
    val initials: String,
    val color: AvatarColor,
    val photoUrl: String?,
)
