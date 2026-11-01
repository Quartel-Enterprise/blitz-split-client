package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.groupBesideGroupsOrNull
import com.quare.blitzsplit.core.navigation.openBesideGroups
import com.quare.blitzsplit.core.navigation.showsOnlyPaneAboveGroups
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearchResults

/*
 * Why: the home's list and detail (a tablet lying down, an unfolded book, an iPad lying down). The UI says whether its
 * window draws the group beside the list; this tells the home when the list is in sight and opens there what the
 * search finds first. Without the pane, the list is in sight only with the groups on top, as on a phone.
 */
internal class GroupsDetailPane(
    private val navigator: Navigator,
) {
    var isShown: Boolean = false
        private set

    // Why: the search's first result the pane last opened, so typing on doesn't reopen it over what was tapped since.
    private var previewedRoute: Route? = null

    // Why: the groups on top, or only the group beside them while the UI draws it there, so the list can be tapped.
    val isListShown: Boolean
        get() {
            val stack = navigator.backStack.value
            return stack.lastOrNull() == Route.Groups || (isShown && stack.showsOnlyPaneAboveGroups())
        }

    // Why: the group the pane beside the list shows, null while the UI draws none.
    val groupIdOrNull: String?
        get() = if (isShown) navigator.backStack.value.groupBesideGroupsOrNull() else null

    /*
     * Why: drawing the group beside the list starts with the search's first result, if it is open and not the one
     * already previewed, so turning the device back never replaces a result tapped since; stopping leaves what is
     * open, which the narrower window draws over the list, as a phone would.
     */
    fun change(
        isShown: Boolean,
        search: GroupsSearch,
    ) {
        if (isShown == this.isShown) return
        this.isShown = isShown
        preview(search)
    }

    // Why: a search closed forgets what it opened, so the next one starts again from its first result.
    fun forgetPreview() {
        previewedRoute = null
    }

    /*
     * Why: beside the list, the search's first result opens as it is typed (5.2): a group or a bill's group as its
     * Despesas, a person as the Membros they were found in. Conta waits for a tap, since a dialog opening by itself
     * would cover what is being typed.
     */
    fun preview(search: GroupsSearch) {
        if (!isShown || !search.isOpen || !search.showsResults || !isListShown) return
        val route = firstResultRouteOf(search.results) ?: return
        if (route == previewedRoute) return
        previewedRoute = route
        navigator.openBesideGroups(route)
    }

    // Why: the sections show Grupos, then Pessoas, then Despesas.
    private fun firstResultRouteOf(results: GroupsSearchResults): Route? {
        results.groups.firstOrNull()?.let { return Route.GroupDetail(it.group.id, joinedAs = null) }
        results.people.firstOrNull()?.let { return Route.Members(it.groupId) }
        return results.bills.firstOrNull()?.let { Route.GroupDetail(it.groupId, joinedAs = null) }
    }
}
