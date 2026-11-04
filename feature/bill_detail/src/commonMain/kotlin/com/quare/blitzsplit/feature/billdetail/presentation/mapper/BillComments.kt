package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentLiker
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import kotlinx.datetime.TimeZone

/*
 * Why: "Comentários" (7.3) in the order they were written, each with its replies under it. A reply whose comment isn't
 * on the device (it never came down) shows as a comment of its own rather than not at all. A deleted comment shows only
 * as "Comentário apagado" over replies still there; a deleted reply, or one with none, is gone.
 */
internal fun threadsOf(
    comments: List<BillComment>,
    group: Group,
    nowMillis: Long,
    timeZone: TimeZone,
): List<BillCommentThread> = threadsOf(comments, group.members, nowMillis, timeZone)

// Why: a bill's link page knows no member, so every comment goes by the name it was written under.
internal fun threadsOf(
    comments: List<BillComment>,
    members: List<GroupMember>,
    nowMillis: Long,
    timeZone: TimeZone,
): List<BillCommentThread> {
    val ids = comments.mapTo(HashSet()) { it.id }
    val (replies, tops) = comments.partition { it.threadId != null && it.threadId in ids }
    val repliesByThread = replies.filterNot { it.isDeleted }.groupBy { it.threadId }
    val rowOf = { comment: BillComment -> rowOf(comment, members, nowMillis, timeZone) }
    return tops.mapNotNull { top ->
        val threadReplies = repliesByThread[top.id].orEmpty()
        if (top.isDeleted && threadReplies.isEmpty()) return@mapNotNull null
        BillCommentThread(
            comment = rowOf(top),
            replies = threadReplies.map(rowOf),
        )
    }
}

// Why: the count beside "Comentários", replies included and "Comentário apagado" not.
internal fun List<BillCommentThread>.countShownComments(): Int =
    sumOf { (if (it.comment.isDeleted) 0 else 1) + it.replies.size }

// Why: an edit that changes nothing would only mark the comment "editado", so Salvar waits for a change.
internal fun List<BillCommentThread>.isUnchangedEdit(
    editingCommentId: String?,
    draft: String,
): Boolean = editingCommentId?.let { findComment(it)?.body == draft.trim() } == true

// Why: a comment shown, top or reply, by its id.
internal fun List<BillCommentThread>.findComment(commentId: String): BillCommentRow? = firstNotNullOfOrNull { thread ->
    (listOf(thread.comment) + thread.replies).firstOrNull { it.id == commentId }
}

// Why: a reply to a reply joins its thread and names whom it answers.
internal fun replyTargetOf(
    threads: List<BillCommentThread>,
    commentId: String,
): BillReplyTarget? {
    val thread = threads.firstOrNull { thread ->
        thread.comment.id == commentId || thread.replies.any { it.id == commentId }
    } ?: return null
    val comment = (listOf(thread.comment) + thread.replies).first { it.id == commentId }
    if (!comment.canInteract) return null
    return BillReplyTarget(
        threadId = thread.comment.id,
        commentId = comment.id,
        name = comment.authorName,
        isMe = comment.isMe,
    )
}

private fun rowOf(
    comment: BillComment,
    members: List<GroupMember>,
    nowMillis: Long,
    timeZone: TimeZone,
): BillCommentRow {
    val member = comment.authorMemberId?.let { id -> members.firstOrNull { it.id == id } }
    val isMe = member?.isMe == true || comment.isMine
    val name = member?.name ?: comment.authorName
    /*
     * Why: the server lets only the author's account change it, so a name released to someone else (Liberar nome)
     * doesn't make its old comments theirs; one not up yet has no account and is this device's member's.
     */
    val myUserId = members.firstOrNull { it.isMe }?.userId
    val isAuthor = comment.isMine || if (comment.authorUserId != null) comment.authorUserId == myUserId else isMe
    // Why: this device's own like is the account's (isMine), whoever its member was.
    val likers = comment.likes.map { like ->
        val liker = like.memberId?.let { id -> members.firstOrNull { it.id == id } }
        BillCommentLiker(
            name = liker?.name ?: like.name,
            isMe = like.isMine,
        )
    }
    return BillCommentRow(
        id = comment.id,
        authorMemberId = member?.id,
        authorName = name,
        initials = name?.let { Initials.ofMember(it, isMe) }.orEmpty(),
        avatarColor = member?.avatarColor ?: AvatarColor.default,
        photoUrl = member?.photoUrl,
        isMe = isMe,
        isAuthor = isAuthor,
        viaLink = comment.viaLink,
        replyToName = comment.replyToName,
        body = comment.body,
        postedAt = comment.postedAt,
        postedDay = SpentDay.relativeDayOf(
            epochMillis = comment.postedAt,
            nowMillis = nowMillis,
            timeZone = timeZone,
        ),
        isEdited = comment.editedAt != null,
        isDeleted = comment.isDeleted,
        likers = if (comment.isDeleted) emptyList() else likers,
        isLikedByMe = !comment.isDeleted && likers.any { it.isMe },
        isPending = comment.isPending,
    )
}
