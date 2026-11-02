package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachment
import kotlinx.coroutines.flow.Flow

fun interface ObserveBillAttachments {
    operator fun invoke(expenseId: String): Flow<List<BillAttachment>>
}
