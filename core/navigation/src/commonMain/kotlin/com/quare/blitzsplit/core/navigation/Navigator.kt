package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.flow.StateFlow

interface Navigator {
    val backStack: StateFlow<List<Route>>

    fun navigate(route: Route)

    fun navigateBack()

    fun replaceAll(route: Route)

    /*
     * Why: a native back gesture (SwiftUI's swipe, the browser's back button) already moved the
     * screen; the back stack only catches up with how many entries remain.
     */
    fun popTo(size: Int)
}
