package com.quare.blitzsplit.core.expenses.domain.model

// Why: where a comment's link points: the bill, and the comment, null for a link to the bill alone.
data class BillCommentTarget(
    val expenseId: String,
    val commentId: String?,
)
