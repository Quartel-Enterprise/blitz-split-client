package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillPayingMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveBillPayingMethodUseCase(
    private val repository: PayingMethodRepository,
) : ObserveBillPayingMethod {
    override fun invoke(expenseId: String): Flow<PayingMethod?> = combine(
        repository.observeMethods(),
        repository.observeMarks(),
    ) { methods, marks ->
        marks[expenseId]?.let { methodId -> methods.firstOrNull { it.id == methodId } }
    }.distinctUntilChanged()
}
