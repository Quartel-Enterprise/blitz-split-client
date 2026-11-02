package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
import kotlinx.datetime.TimeZone

/*
 * Why: "Comentários" (7.3) in the order they were written, each with its replies under it. A reply whose comment isn't
 * on the device (it never came down) shows as a comment of its own rather than not at all.
 */
internal fun threadsOf(
    comments: List<BillComment>,
    group: Group,
    nowMillis: Long,
    timeZone: TimeZone,
): List<BillCommentThread> {
    val ids = comments.mapTo(HashSet()) { it.id }
    val (replies, tops) = comments.partition { it.threadId != null && it.threadId in ids }
    val repliesByThread = replies.groupBy { it.threadId }
    val rowOf = { comment: BillComment -> rowOf(comment, group, nowMillis, timeZone) }
    return tops.map { top ->
        BillCommentThread(
            comment = rowOf(top),
            replies = repliesByThread[top.id].orEmpty().map(rowOf),
        )
    }
}

private fun rowOf(
    comment: BillComment,
    group: Group,
    nowMillis: Long,
    timeZone: TimeZone,
): BillCommentRow {
    val member = comment.authorMemberId?.let { id -> group.members.firstOrNull { it.id == id } }
    val isMe = member?.isMe == true
    val name = member?.name ?: comment.authorName
    return BillCommentRow(
        id = comment.id,
        authorName = name,
        initials = name?.let { Initials.ofMember(it, isMe) }.orEmpty(),
        avatarColor = member?.avatarColor ?: AvatarColor.default,
        photoUrl = member?.photoUrl,
        isMe = isMe,
        viaLink = comment.viaLink,
        replyToName = comment.replyToName,
        body = comment.body,
        postedAt = comment.postedAt,
        postedDay = SpentDay.relativeDayOf(
            epochMillis = comment.postedAt,
            nowMillis = nowMillis,
            timeZone = timeZone,
        ),
        isPending = comment.isPending,
    )
}
