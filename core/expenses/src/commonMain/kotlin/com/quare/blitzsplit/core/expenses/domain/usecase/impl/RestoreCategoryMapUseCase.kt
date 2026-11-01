package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.DefaultCategoryTerms
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.RestoreCategoryMap
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class RestoreCategoryMapUseCase(
    private val repository: CategoryTermRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : RestoreCategoryMap {
    override suspend fun invoke(): Result<Unit> = repository.restoreAppTerms(
        appKeys = DefaultCategoryTerms.terms.map { CategoryMatcher.keyOf(it.term) }.toSet(),
        updatedAt = currentTimeMillis(),
    )
}
