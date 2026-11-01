package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: a group "Buscar" found, drawn as its row on the home says it ("9 membros · 3 dias", "+R$ 97,50"). A settled
 * group also comes by one of its bills, "Quitado · despesa “Churros”", since its bills don't list under Despesas.
 */
data class SearchGroupHit(
    val group: GroupRow,
    // Why: null when the group came by a bill.
    val nameMatch: TextMatch?,
    // Why: the most recent of the settled group's bills the search found, when its name didn't match.
    val billDescription: String?,
    val billMatch: TextMatch?,
)
