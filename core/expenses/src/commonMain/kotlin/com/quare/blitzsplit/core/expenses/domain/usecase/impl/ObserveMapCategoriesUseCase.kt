package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveMapCategoriesUseCase(
    private val repository: CategoryRepository,
) : ObserveMapCategories {
    override fun invoke(): Flow<CategoryTree> = repository
        .observeAllGroupCategories()
        .map(CategoryTree::ofGroups)
}
