package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import kotlinx.coroutines.flow.Flow

fun interface ObserveMapCategories {
    // Why: the categories a term may send titles to (9.2): the app's and those of every group on the device.
    operator fun invoke(): Flow<CategoryTree>
}
