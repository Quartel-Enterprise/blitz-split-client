package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import kotlinx.coroutines.flow.Flow

fun interface ObserveBillComments {
    operator fun invoke(expenseId: String): Flow<List<BillComment>>
}
