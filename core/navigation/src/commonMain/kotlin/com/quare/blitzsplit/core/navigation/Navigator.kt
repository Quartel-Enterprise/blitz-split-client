package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.flow.StateFlow

interface Navigator {
    val backStack: StateFlow<List<Route>>

    fun navigate(route: Route)

    fun navigateBack()

    // Why: one screen follows another in its place, so back skips the first ("Novo grupo" becomes "Convidar").
    fun replaceTop(route: Route)

    fun replaceAll(route: Route)

    /*
     * Why: keeps the first size entries and pushes route over them in one step, so a UI never draws the stack in
     * between: a group tapped in a list and detail takes the place of the group shown beside the list.
     */
    fun replaceAbove(
        size: Int,
        route: Route,
    )

    // Why: the same with several routes, bottom first: a group the sidebar opens from Início puts Grupos under it.
    fun replaceAbove(
        size: Int,
        routes: List<Route>,
    )

    /*
     * Why: a native back gesture (SwiftUI's swipe, the browser's back button) already moved the
     * screen; the back stack only catches up with how many entries remain.
     */
    fun popTo(size: Int)
}
