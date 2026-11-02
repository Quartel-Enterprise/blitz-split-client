package com.quare.blitzsplit.core.expenses.domain.model

// Why: what the server takes (`bill_comments.body`), trimmed.
object BillCommentRules {
    const val MAX_LENGTH = 1_000

    fun isBodyValid(body: String): Boolean = body.trim().length in 1..MAX_LENGTH
}
