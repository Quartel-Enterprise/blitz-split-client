package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.SettledUp
import kotlinx.coroutines.flow.Flow

// Why: the notice the global Saldos owes once Registrar pagamento settled up with someone over it. Only while the app runs.
interface SettledUpRepository {
    fun observeSettledUp(): Flow<SettledUp?>

    fun post(settledUp: SettledUp)

    // Why: only that notice, so one posted meanwhile keeps its own.
    fun consume(settledUp: SettledUp)
}
