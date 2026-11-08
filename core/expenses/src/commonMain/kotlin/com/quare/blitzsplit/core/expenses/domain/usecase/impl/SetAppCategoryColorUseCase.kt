package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SetAppCategoryColor

class SetAppCategoryColorUseCase(
    private val repository: CategoryRepository,
) : SetAppCategoryColor {
    override suspend fun invoke(
        categoryKey: String,
        color: CategoryColor?,
    ): Result<Unit> {
        val category = AppCategory.fromKeyOrNull(categoryKey)
        if (category == null || !category.isMain) {
            return Result.failure(IllegalArgumentException("Not an app main category: $categoryKey"))
        }
        // Why: the app's default is stored as none, so a later change of the default reaches this person too.
        val stored = color?.takeIf { it != category.defaultColor }
        return repository.saveAppCategoryColor(categoryKey, stored)
    }
}
