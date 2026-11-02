package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadBillAttachmentImage

class LoadBillAttachmentImageUseCase(
    private val repository: BillAttachmentRepository,
) : LoadBillAttachmentImage {
    override suspend fun invoke(id: String): Result<ByteArray?> = repository.getPendingImage(id)
}
