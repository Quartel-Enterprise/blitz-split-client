package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.DeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone

class NoticeBillGoneUseCase(
    private val repository: DeletedBillRepository,
) : NoticeBillGone {
    override fun invoke(
        groupId: String,
        expenseId: String,
    ) {
        if (!repository.isDeletedHere(expenseId)) repository.postNotice(groupId)
    }
}
