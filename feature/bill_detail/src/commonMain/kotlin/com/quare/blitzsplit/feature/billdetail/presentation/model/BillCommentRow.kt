package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor

// Why: one comment, "Carla · ontem, 22:40"; each UI says "Você" for isMe.
data class BillCommentRow(
    val id: String,
    // Why: the author while still in the group, whose face and name open their profile; null for anyone else.
    val authorMemberId: String?,
    // Why: the member's name as it is now, else the name it was written under; null when the server kept none.
    val authorName: String?,
    val initials: String,
    val avatarColor: AvatarColor,
    // Why: the member's photo in the face, null for none or someone out of the group.
    val photoUrl: String?,
    val isMe: Boolean,
    // Why: "Pelo link", written by someone out of the group through the bill's open link.
    val viaLink: Boolean,
    // Why: whom a reply to a reply answers, "@Diego".
    val replyToName: String?,
    // Why: empty for a deleted comment.
    val body: String,
    val postedAt: Long,
    val postedDay: RelativeDay,
    // Why: "editado" after the time.
    val isEdited: Boolean,
    // Why: "Comentário apagado", a deleted top comment kept over its replies, with no face, name nor actions.
    val isDeleted: Boolean,
    // Why: "♥ 3", whoever liked it in the order they did, "Você" for isMe.
    val likers: List<BillCommentLiker>,
    // Why: the heart filled.
    val isLikedByMe: Boolean,
    // Why: "Na fila · sai ao reconectar".
    val isPending: Boolean,
) {
    val likeCount: Int
        get() = likers.size

    // Why: only its author edits or deletes it, never once deleted.
    val canChange: Boolean
        get() = isMe && !isDeleted

    // Why: a deleted comment takes no like, reply nor share.
    val canInteract: Boolean
        get() = !isDeleted
}
