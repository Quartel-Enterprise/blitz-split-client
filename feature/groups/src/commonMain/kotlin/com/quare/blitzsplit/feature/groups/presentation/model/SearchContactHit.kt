package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: a person "Search" found, once however many groups they are in, which opens their profile, the one Contacts
 * opens (Pierre's beta feedback): personKey is PeopleBalances.keyOf. Above the same person's rows in each group, which
 * open that group's Members.
 */
data class SearchContactHit(
    val personKey: String,
    val name: String,
    val nameMatch: TextMatch,
    val initial: String,
    val color: AvatarColor,
    val photoUrl: String?,
    // Why: "Contact · 3 groups", every group in common with them, as their profile counts them.
    val groupCount: Int,
)
