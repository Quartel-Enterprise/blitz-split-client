package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveCategoryTerm
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

// Why: on the device right away, offline included; the person's other devices get it once synced.
class SaveCategoryTermUseCase(
    private val repository: CategoryTermRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : SaveCategoryTerm {
    override suspend fun invoke(
        term: String,
        categoryId: String,
        replacedTerm: String?,
    ): Result<Unit> {
        if (!CategoryMatcher.isTermValid(term)) return Result.failure(IllegalArgumentException("Invalid term"))
        val now = currentTimeMillis()
        val isRenamed = replacedTerm != null && CategoryMatcher.keyOf(replacedTerm) != CategoryMatcher.keyOf(term)
        if (isRenamed) {
            repository
                .saveTerm(
                    term = replacedTerm,
                    categoryId = null,
                    updatedAt = now,
                ).onFailure { return Result.failure(it) }
        }
        return repository.saveTerm(
            term = term,
            categoryId = categoryId,
            updatedAt = now,
        )
    }
}
