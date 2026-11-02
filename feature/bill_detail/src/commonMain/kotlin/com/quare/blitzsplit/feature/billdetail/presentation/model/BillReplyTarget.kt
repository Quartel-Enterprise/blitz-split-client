package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: "Respondendo a Diego" over the composer, with its ✕.
data class BillReplyTarget(
    // Why: the thread's top comment, which the reply joins.
    val threadId: String,
    // Why: the comment answered, the top one or a reply in it.
    val commentId: String,
    val name: String?,
    val isMe: Boolean,
) {
    // Why: a reply to a reply names whom it answers; one to the thread's top comment doesn't need to.
    val answeredName: String?
        get() = name.takeIf { commentId != threadId }
}
