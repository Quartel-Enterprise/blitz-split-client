package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryTeachRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: the question only matters while the app runs.
class InMemoryCategoryTeachRepository : CategoryTeachRepository {
    private val offer = MutableStateFlow<CategoryTeachOffer?>(null)

    override fun observeOffer(): Flow<CategoryTeachOffer?> = offer

    override fun post(offer: CategoryTeachOffer) {
        this.offer.value = offer
    }

    override fun consume(expenseId: String) {
        offer.update { if (it?.expenseId == expenseId) null else it }
    }
}
