package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTeachOffer
import kotlinx.coroutines.flow.Flow

// Why: "Ensinar ao mapa?" (9.3e) from Rachar conta to the group's Despesas, only while the app runs.
interface CategoryTeachRepository {
    fun observeOffer(): Flow<CategoryTeachOffer?>

    fun post(offer: CategoryTeachOffer)

    // Why: only that bill's offer, so one posted meanwhile keeps its own.
    fun consume(expenseId: String)
}
