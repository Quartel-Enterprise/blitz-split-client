package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.provider.image.BillImage

fun interface AttachBillImage {
    /*
     * Why: an image already encoded as the bucket takes it (a receipt read, 7.1e, or a photo framed in "Ajustar foto"),
     * stored as it is, never encoded again; the member is this device's.
     */
    suspend operator fun invoke(
        expenseId: String,
        groupId: String,
        kind: BillAttachmentKind,
        image: BillImage,
        addedByMemberId: String?,
    ): Result<BillAttachmentAddition>
}
