package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

data class NewGroupMember(
    val name: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
    /*
     * Why: someone picked from Contacts who uses the app goes in as their account, joined, as Contacts' "Add to a
     * group" adds them; null for a name typed (a guest) and for this device's user, whose account the device knows.
     */
    val userId: String?,
)
