package com.quare.blitzsplit.core.groups.domain.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

// Why: the group behind an invite link, as whoever opens it sees it before joining.
data class Invite(
    val code: String,
    val groupId: String,
    val groupName: String,
    val groupType: GroupType,
    val groupColor: AvatarColor,
    val currency: GroupCurrency,
    // Why: the name of the creator's member, null once that account was deleted.
    val creatorName: String?,
    // Why: this account already joined the group, so the link opens the group instead.
    val isMember: Boolean,
    // Why: in the order they were added, so the creator comes first.
    val members: List<InviteMember>,
    // Why: the member a personal link joins as, null for the group's link and once that member was removed.
    val personalMemberId: String?,
    // Why: "Ana te chamou como Carla", whoever shared a personal link, null when the link doesn't say.
    val inviterName: String?,
)
