package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import kotlinx.coroutines.flow.Flow

fun interface ObservePayingMethods {
    // Why: the person's payment methods, in the order they were added, the default first.
    operator fun invoke(): Flow<List<PayingMethod>>
}
