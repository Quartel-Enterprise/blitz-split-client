package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment

fun interface LoadBillAttachmentUrls {
    // Why: by the attachment's id, for those already in the bucket; needs the internet.
    suspend operator fun invoke(attachments: List<BillAttachment>): Result<Map<String, String>>
}
