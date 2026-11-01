package com.quare.blitzsplit.feature.groups.presentation.model

// Why: the sections "Grupos", "Pessoas" and "Despesas", only the ones the scope picked holding anything.
data class GroupsSearchResults(
    // Why: in the home's order, the most recent activity first, the settled groups included.
    val groups: List<SearchGroupHit>,
    // Why: by their group, in the home's order, then in the order they were added to it.
    val people: List<SearchPersonHit>,
    // Why: the most recent "Quando" first.
    val bills: List<SearchBillHit>,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty() && people.isEmpty() && bills.isEmpty()

    companion object {
        val none: GroupsSearchResults = GroupsSearchResults(
            groups = emptyList(),
            people = emptyList(),
            bills = emptyList(),
        )
    }
}
