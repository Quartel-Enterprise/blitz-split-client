package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget

// Why: what the composer holds: the comment being edited, if any, the text and the reply it answers.
internal data class CommentComposer(
    val editingCommentId: String?,
    val draft: String,
    val replyTarget: BillReplyTarget?,
)
