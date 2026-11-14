package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import kotlinx.coroutines.flow.Flow

fun interface ObserveBillPayingMethod {
    // Why: the method this device's user marked the bill with, null for none or one since deleted.
    operator fun invoke(expenseId: String): Flow<PayingMethod?>
}
