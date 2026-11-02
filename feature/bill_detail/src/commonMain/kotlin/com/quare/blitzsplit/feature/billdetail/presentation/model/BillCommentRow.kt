package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor

// Why: one comment, "Carla · ontem, 22:40"; each UI says "Você" for isMe.
data class BillCommentRow(
    val id: String,
    // Why: the member's name as it is now, else the name it was written under; null when the server kept none.
    val authorName: String?,
    val initials: String,
    val avatarColor: AvatarColor,
    val isMe: Boolean,
    // Why: "Pelo link", written by someone out of the group through the bill's open link.
    val viaLink: Boolean,
    // Why: whom a reply to a reply answers, "@Diego".
    val replyToName: String?,
    val body: String,
    val postedAt: Long,
    val postedDay: RelativeDay,
    // Why: "Na fila · sai ao reconectar".
    val isPending: Boolean,
)
