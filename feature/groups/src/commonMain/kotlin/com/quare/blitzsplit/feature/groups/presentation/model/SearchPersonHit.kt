package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: a member "Buscar" found, once per group they are in; this device's user never, who is in every group.
data class SearchPersonHit(
    val memberId: String,
    // Why: PeopleBalances.keyOf, which gathers the person's rows once under contacts.
    val personKey: String,
    val name: String,
    val nameMatch: TextMatch,
    val initial: String,
    val color: AvatarColor,
    // Why: the profile photo over the initials, null for someone with none or nobody joined as.
    val photoUrl: String?,
    val groupId: String,
    val groupName: String,
)
