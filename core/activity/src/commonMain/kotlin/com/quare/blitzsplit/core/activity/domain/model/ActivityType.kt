package com.quare.blitzsplit.core.activity.domain.model

// Why: the five types "Filtrar" picks from: Contas, Pagamentos e acertos, Comentários, Pessoas e grupos, Cobranças.
enum class ActivityType {
    BILLS,
    PAYMENTS,

    // Why: in the filter already, though nothing makes a comment yet (bills have no comments).
    COMMENTS,
    PEOPLE_AND_GROUPS,
    CHARGES,
}
