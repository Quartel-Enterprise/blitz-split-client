package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: what a profile link shows anyone, signed out too: the name and the photo. The account only comes with it for
 * the person themself and for whoever already shares a group with them, who has them in Contacts.
 */
data class SharedProfile(
    val displayName: String,
    val avatarColor: AvatarColor,
    val photoUrl: String?,
    val userId: String?,
)
