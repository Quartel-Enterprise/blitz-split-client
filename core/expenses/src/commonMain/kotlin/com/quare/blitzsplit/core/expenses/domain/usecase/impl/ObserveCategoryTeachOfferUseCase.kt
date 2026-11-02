package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTeachRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTeachOffer
import kotlinx.coroutines.flow.Flow

class ObserveCategoryTeachOfferUseCase(
    private val repository: CategoryTeachRepository,
) : ObserveCategoryTeachOffer {
    override fun invoke(): Flow<CategoryTeachOffer?> = repository.observeOffer()
}
