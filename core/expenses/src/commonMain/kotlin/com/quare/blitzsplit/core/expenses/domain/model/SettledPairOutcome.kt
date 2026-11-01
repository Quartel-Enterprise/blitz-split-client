package com.quare.blitzsplit.core.expenses.domain.model

// Why: how a pair that had settled up stands once a bill's change counts, which each UI words.
enum class SettledPairOutcome {
    // Why: "Elisa volta a dever R$ 20,00 a Bruno": whoever paid owes again what the payment had settled.
    OWES_AGAIN,

    // Why: "Bruno passa a dever R$ 98,00 a Elisa": the other way round, or more of what was still owed.
    OWES,

    // Why: "Elisa e Bruno ficam quitados".
    EVEN,
}
