package com.quare.blitzsplit.feature.globalbalances.presentation.model

enum class BalancesEmptyState {
    // Why: "Tudo acertado", with "Ver grupos".
    SETTLED,

    // Why: "Você não deve nada" on Você deve, with "Ver quem te deve".
    NOTHING_TO_PAY,

    // Why: nobody owes you on Te devem while you still owe someone, with "Ver quem você deve".
    NOTHING_TO_RECEIVE,
}
