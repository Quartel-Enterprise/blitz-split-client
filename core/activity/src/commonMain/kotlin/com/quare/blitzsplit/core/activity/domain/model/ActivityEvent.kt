package com.quare.blitzsplit.core.activity.domain.model

/*
 * Why: one item of "Atividade", as the server recorded it. The snapshot fields keep it readable once its target or its
 * group is gone: who did it and in which group by name, and the target's description, amount or name.
 */
data class ActivityEvent(
    val id: String,
    // Why: the server's order and what the read mark counts; null for a charge not up yet.
    val seq: Long?,
    val groupId: String,
    val kind: ActivityKind,
    val actorUserId: String?,
    val actorMemberId: String?,
    val actorName: String?,
    val groupName: String?,
    val targetId: String?,
    val occurredAt: Long,
    // Why: the bill's description, or the group's or the member's name, as it was then.
    val subject: String?,
    val amountMinor: Long?,
    // Why: a bill's split mode, a group's type, a payment's method, an attachment's kind, a link's visibility, by codes.
    val code: String?,
    val fromMemberId: String?,
    val toMemberId: String?,
    val changes: List<ActivityChange>,
    // Why: how many take part in a new bill ("Igual entre 5").
    val sharesCount: Int?,
    // Why: a comment's text as it reads now, cut to what a feed row shows; null once deleted.
    val body: String?,
    // Why: the comment a comment_added event is about, which its tap scrolls to.
    val commentId: String?,
    // Why: "Ana apagou um comentário em Jantar": its author deleted it.
    val isCommentDeleted: Boolean,
    // Why: a charge sent here and not up yet.
    val isPending: Boolean,
)
