package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTeachRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.OfferCategoryTeach

class OfferCategoryTeachUseCase(
    private val repository: CategoryTeachRepository,
) : OfferCategoryTeach {
    override fun invoke(offer: CategoryTeachOffer) {
        repository.post(offer)
    }
}
