package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

fun interface ObserveRecordedPayment {
    // Why: the payment just recorded in the group, owed "Pagamento de Ana registrado"; null when none is.
    operator fun invoke(groupId: String): Flow<Settlement?>
}
