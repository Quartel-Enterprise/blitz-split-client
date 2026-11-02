package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import kotlinx.coroutines.flow.Flow

fun interface ObserveCategoryTree {
    // Why: the app's categories with the group's among them, as a sync or a change here moves them.
    operator fun invoke(groupId: String): Flow<CategoryTree>
}
