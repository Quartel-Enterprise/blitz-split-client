package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface AddBillComment {
    // Why: threadId is the comment answered (a reply to a reply joins its thread); the author is this device's member.
    suspend operator fun invoke(
        expenseId: String,
        groupId: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorMemberId: String?,
        authorName: String?,
    ): Result<Unit>
}
