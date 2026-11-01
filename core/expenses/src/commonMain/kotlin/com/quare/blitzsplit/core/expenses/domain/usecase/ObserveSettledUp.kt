package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import kotlinx.coroutines.flow.Flow

fun interface ObserveSettledUp {
    // Why: the settling up just recorded, owed "Pagamento de Carla registrado"; null when none is.
    operator fun invoke(): Flow<RecordedPayment.SettledUp?>
}
