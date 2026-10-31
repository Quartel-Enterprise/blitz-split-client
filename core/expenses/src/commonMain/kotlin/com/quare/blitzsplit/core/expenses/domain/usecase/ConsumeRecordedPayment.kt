package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface ConsumeRecordedPayment {
    // Why: the notice was shown, so no other screen shows it again.
    operator fun invoke(settlementId: String)
}
