package com.quare.blitzsplit.core.activity.domain.model

// Why: the five types "Filtrar" picks from: Contas, Pagamentos e acertos, Comentários, Pessoas e grupos, Cobranças.
enum class ActivityType {
    BILLS,
    PAYMENTS,

    // Why: a bill's comments (7.3).
    COMMENTS,
    PEOPLE_AND_GROUPS,
    CHARGES,
}
