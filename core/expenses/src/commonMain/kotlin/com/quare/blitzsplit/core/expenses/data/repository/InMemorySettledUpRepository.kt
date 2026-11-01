package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import com.quare.blitzsplit.core.expenses.domain.repository.SettledUpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Why: a notice only matters while the app runs.
class InMemorySettledUpRepository : SettledUpRepository {
    private val settledUp = MutableStateFlow<SettledUp?>(null)

    override fun observeSettledUp(): Flow<SettledUp?> = settledUp

    override fun post(settledUp: SettledUp) {
        this.settledUp.value = settledUp
    }

    override fun consume(settledUp: SettledUp) {
        this.settledUp.update { if (it == settledUp) null else it }
    }
}
