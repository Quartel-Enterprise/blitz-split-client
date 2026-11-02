package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadBillAttachmentUrls

class LoadBillAttachmentUrlsUseCase(
    private val repository: BillAttachmentRepository,
) : LoadBillAttachmentUrls {
    override suspend fun invoke(attachments: List<BillAttachment>): Result<Map<String, String>> =
        repository.getImageUrls(attachments)
}
