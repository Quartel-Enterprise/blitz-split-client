package com.quare.blitzsplit.core.expenses.domain.model

// Why: what the server takes (`bill_comments.body`), trimmed, in letters as its char_length counts them (an emoji is one).
object BillCommentRules {
    const val MAX_LENGTH = 1_000

    fun isBodyValid(body: String): Boolean {
        val trimmed = body.trim()
        return trimmed.length - trimmed.count { it.isLowSurrogate() } in 1..MAX_LENGTH
    }
}
