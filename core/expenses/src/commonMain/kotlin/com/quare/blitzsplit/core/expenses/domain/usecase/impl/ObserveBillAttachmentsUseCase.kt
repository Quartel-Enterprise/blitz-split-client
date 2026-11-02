package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillAttachments
import kotlinx.coroutines.flow.Flow

class ObserveBillAttachmentsUseCase(
    private val repository: BillAttachmentRepository,
) : ObserveBillAttachments {
    override fun invoke(expenseId: String): Flow<List<BillAttachment>> = repository.observeAttachments(expenseId)
}
