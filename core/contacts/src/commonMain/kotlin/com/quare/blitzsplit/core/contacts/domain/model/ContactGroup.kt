package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: one of "Grupos em comum", with what that group adds to the net with the person.
data class ContactGroup(
    val groupId: String,
    val groupName: String,
    val groupInitials: String,
    val groupColor: AvatarColor,
    val currency: GroupCurrency,
    // Why: who the person is in that group, and who this device's user is.
    val memberId: String,
    val meMemberId: String,
    // Why: the codes of a guest's personal link ("Convidar Diego"), null until the group's first sync brings them.
    val groupInviteCode: String?,
    val memberInviteCode: String?,
    val meInviteCode: String?,
    // Why: positive when they owe you in this group, negative when you owe them, zero when settled.
    val amountMinor: Long,
)
