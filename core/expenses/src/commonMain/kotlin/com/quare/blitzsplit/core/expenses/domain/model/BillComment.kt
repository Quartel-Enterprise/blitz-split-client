package com.quare.blitzsplit.core.expenses.domain.model

// Why: one comment on a bill ("Comentários", 7.3), as written on a device or as the server stamped it.
data class BillComment(
    val id: String,
    val expenseId: String,
    val groupId: String,
    val authorUserId: String?,
    // Why: null for someone who commented through the bill's open link without being in the group ("Pelo link").
    val authorMemberId: String?,
    val authorName: String?,
    // Why: the top comment of its thread, null for a top comment; replies are one level deep.
    val threadId: String?,
    // Why: whom a reply to a reply answers.
    val replyToName: String?,
    val body: String,
    val viaLink: Boolean,
    val postedAt: Long,
    // Why: written here and not up yet: "Na fila · sai ao reconectar".
    val isPending: Boolean,
)
