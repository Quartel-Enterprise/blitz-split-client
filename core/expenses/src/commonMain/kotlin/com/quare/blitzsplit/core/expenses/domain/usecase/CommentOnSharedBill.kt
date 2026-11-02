package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment

fun interface CommentOnSharedBill {
    /*
     * Why: id is the caller's, one per comment typed, so sending it again after a lost answer saves it once; threadId
     * is the comment answered, as in Conta; authorName is this device's profile's, for a server that knows no name of
     * whoever signed in.
     */
    suspend operator fun invoke(
        code: String,
        id: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorName: String?,
    ): Result<SharedBillComment>
}
