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
     * Why: a native back gesture (SwiftUI's swipe, the browser's back button) already moved the
     * screen; the back stack only catches up with how many entries remain.
     */
    fun popTo(size: Int)
}
