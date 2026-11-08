package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveCategoryTreeUseCase(
    private val repository: CategoryRepository,
) : ObserveCategoryTree {
    override fun invoke(groupId: String): Flow<CategoryTree> = combine(
        repository.observeGroupCategories(groupId),
        repository.observeAppColors(),
        CategoryTree::of,
    )
}
