package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.DeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeDeletedBillNotice

class ConsumeDeletedBillNoticeUseCase(
    private val repository: DeletedBillRepository,
) : ConsumeDeletedBillNotice {
    override fun invoke(groupId: String) {
        repository.consumeNotice(groupId)
    }
}
