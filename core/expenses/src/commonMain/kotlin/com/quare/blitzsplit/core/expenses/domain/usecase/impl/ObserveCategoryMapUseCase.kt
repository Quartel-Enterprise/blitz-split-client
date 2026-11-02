package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTermRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveCategoryMapUseCase(
    private val repository: CategoryTermRepository,
) : ObserveCategoryMap {
    override fun invoke(): Flow<CategoryMap> = repository
        .observeChanges()
        .map { CategoryMap.of(it) }
        .distinctUntilChanged()
}
