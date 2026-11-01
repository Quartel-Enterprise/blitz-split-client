package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import kotlinx.coroutines.flow.Flow

fun interface ObserveCategoryMap {
    // Why: the person's map as it counts now, in every group and offline too (flow 9).
    operator fun invoke(): Flow<CategoryMap>
}
