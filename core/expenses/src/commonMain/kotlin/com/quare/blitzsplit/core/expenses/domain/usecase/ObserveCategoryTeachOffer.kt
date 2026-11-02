package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import kotlinx.coroutines.flow.Flow

fun interface ObserveCategoryTeachOffer {
    operator fun invoke(): Flow<CategoryTeachOffer?>
}
