package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.RemoveBillAttachment
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

// Why: any member takes a photo off, offline too; Histórico says who did once the server has it.
class RemoveBillAttachmentUseCase(
    private val repository: BillAttachmentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : RemoveBillAttachment {
    override suspend fun invoke(id: String): Result<Unit> = repository.removeAttachment(id, currentTimeMillis())
}
