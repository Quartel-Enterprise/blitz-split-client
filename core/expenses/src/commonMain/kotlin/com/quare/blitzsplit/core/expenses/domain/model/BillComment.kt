package com.quare.blitzsplit.core.expenses.domain.model

// Why: one comment on a bill ("Comentários", 7.3), as written on a device or as the server stamped it.
// Only its author edits or deletes it (beta feedback 14).
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
    // Why: empty once deleted.
    val body: String,
    val viaLink: Boolean,
    val postedAt: Long,
    // Why: "editado", when its author last edited it; null while never edited.
    val editedAt: Long?,
    // Why: deleted by its author; a top comment with replies still shows as "Comentário apagado" over them.
    val isDeleted: Boolean,
    /*
     * Why: the server says this device's account wrote it, which only a bill's link page needs: in the app the author's
     * member says so.
     */
    val isMine: Boolean,
    // Why: "♥ 3", who liked it, in the order they did.
    val likes: List<BillCommentLike>,
    // Why: written, edited or deleted here and not up yet: "Na fila · sai ao reconectar".
    val isPending: Boolean,
)
