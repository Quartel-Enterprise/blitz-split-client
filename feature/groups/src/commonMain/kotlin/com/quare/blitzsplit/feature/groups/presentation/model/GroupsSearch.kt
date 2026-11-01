package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: "Buscar" (5.2), over the groups, people and bills on this device, the settled groups included, offline too:
 * Android's search bar, iOS's search tab, the web's ⌘K and the Mac's toolbar field.
 */
data class GroupsSearch(
    val isOpen: Boolean,
    // Why: what was typed, kept while the search is open, so rotating or folding the device doesn't lose it.
    val query: String,
    val scope: SearchScope,
    val results: GroupsSearchResults,
) {
    val hasQuery: Boolean
        get() = query.isNotBlank()

    val showsResults: Boolean
        get() = hasQuery && !results.isEmpty

    // Why: "Nada com “pizza”".
    val showsNoResults: Boolean
        get() = hasQuery && results.isEmpty

    // Why: "Nada com “pizza”" names what was looked for as typed, without the spaces around it.
    val trimmedQuery: String
        get() = query.trim()

    companion object {
        val closed: GroupsSearch = GroupsSearch(
            isOpen = false,
            query = "",
            scope = SearchScope.ALL,
            results = GroupsSearchResults.none,
        )
    }
}
