package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SettledUp

fun interface ConsumeSettledUp {
    // Why: the notice shown, so it doesn't show again.
    operator fun invoke(settledUp: SettledUp)
}
