package com.quare.blitzsplit.feature.activity.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

// Why: who did it, as their member in the group: "Você" for this device's user, whom each UI names.
data class ActivityActor(
    // Why: null when the server kept no name (an account deleted before it had a member).
    val name: String?,
    val isMe: Boolean,
    val initials: String,
    val color: AvatarColor,
    // Why: their profile photo over the initials, null for no photo or nobody in a group on the device.
    val photoUrl: String?,
    // Why: PeopleBalances.keyOf while they are in a group on the device, whose profile their face opens; else null.
    val personKey: String?,
) {
    // Why: the face opens their profile, or Perfil for this device's user; nobody else has one to open.
    val opensProfile: Boolean
        get() = isMe || personKey != null
}
