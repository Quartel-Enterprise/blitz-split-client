package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.model.AvatarColor

// Why: a person of "Por pessoa" and their slice of its ring; a null name is someone who left the group ("Ex-membro").
data class StatsPersonRow(
    val memberId: String,
    val name: String?,
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: the profile photo over the initials, null for someone with none or nobody joined as.
    val photoUrl: String?,
    val isMe: Boolean,
    val amountMinor: Long,
    // Why: their share of the view's total, rounded so the list adds up to 100 (0 for everyone while nothing was spent).
    val percent: Int,
    /*
     * Why: their slice and the dot before their name, Default split's color by their place in the group's list
     * (SplitBarColors), so a person reads in one color on both screens; Gray for a former member, who has no place.
     */
    val color: CategoryColor,
) {
    // Why: the row opens their profile (Perfil for isMe); someone no longer in the group has none.
    val opensProfile: Boolean
        get() = name != null
}
