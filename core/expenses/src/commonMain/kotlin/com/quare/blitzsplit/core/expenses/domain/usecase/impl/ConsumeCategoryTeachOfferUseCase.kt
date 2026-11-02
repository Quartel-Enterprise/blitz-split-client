package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTeachRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeCategoryTeachOffer

class ConsumeCategoryTeachOfferUseCase(
    private val repository: CategoryTeachRepository,
) : ConsumeCategoryTeachOffer {
    override fun invoke(expenseId: String) {
        repository.consume(expenseId)
    }
}
