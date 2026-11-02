package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer

fun interface OfferCategoryTeach {
    operator fun invoke(offer: CategoryTeachOffer)
}
