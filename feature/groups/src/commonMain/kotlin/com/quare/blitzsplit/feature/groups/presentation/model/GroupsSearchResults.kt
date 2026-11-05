package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: the sections "Groups", "People" and "Expenses", only the ones the scope picked holding anything: what holds the
 * text typed, or, when nothing does, what comes close (a typo or two away, TypoTolerantQuery), the closest first.
 */
data class GroupsSearchResults(
    // Why: in the home's order, the most recent activity first, the settled groups included.
    val groups: List<SearchGroupHit>,
    // Why: each person once, opening their profile, in the order of their first row under people.
    val contacts: List<SearchContactHit>,
    // Why: by their group, in the home's order, then in the order they were added to it.
    val people: List<SearchPersonHit>,
    // Why: the most recent "Quando" first.
    val bills: List<SearchBillHit>,
    /*
     * Why: "Did you mean “Larissa”?" over what comes close when nothing holds the text typed: the closest name or
     * description as written, which a tap searches instead. Null while anything holds the text typed.
     */
    val suggestion: String?,
) {
    val isEmpty: Boolean
        get() = groups.isEmpty() && contacts.isEmpty() && people.isEmpty() && bills.isEmpty()

    companion object {
        val none: GroupsSearchResults = GroupsSearchResults(
            groups = emptyList(),
            contacts = emptyList(),
            people = emptyList(),
            bills = emptyList(),
            suggestion = null,
        )
    }
}
