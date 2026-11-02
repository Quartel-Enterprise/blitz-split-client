package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.RemoveCategoryTerm
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class RemoveCategoryTermUseCase(
    private val repository: CategoryTermRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : RemoveCategoryTerm {
    override suspend fun invoke(term: String): Result<Unit> = repository.saveTerm(
        term = term,
        categoryId = null,
        updatedAt = currentTimeMillis(),
    )
}
