package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: a comment and its replies, one level deep, in the order they were written.
data class BillCommentThread(
    val comment: BillCommentRow,
    val replies: List<BillCommentRow>,
)
