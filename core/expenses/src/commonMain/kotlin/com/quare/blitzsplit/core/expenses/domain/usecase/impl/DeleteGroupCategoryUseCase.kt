package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteGroupCategory
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class DeleteGroupCategoryUseCase(
    private val repository: CategoryRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : DeleteGroupCategory {
    override suspend fun invoke(categoryId: String): Result<Unit> = repository.deleteGroupCategory(
        id = categoryId,
        deletedAt = currentTimeMillis(),
    )
}
