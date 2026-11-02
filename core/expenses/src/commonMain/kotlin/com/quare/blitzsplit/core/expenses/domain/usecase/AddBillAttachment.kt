package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind

fun interface AddBillAttachment {
    // Why: source is the picked file as it came (a camera's photo, a gallery's or a receipt's image); the member is this device's.
    suspend operator fun invoke(
        expenseId: String,
        groupId: String,
        kind: BillAttachmentKind,
        source: ByteArray,
        addedByMemberId: String?,
    ): Result<BillAttachmentAddition>
}
