package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/*
 * Why: the Mac's mouse back and forward buttons (and Go › Back and Forward) walk the back stack as a browser's history
 * does (Pierre's beta feedback): back closes the screen on top, forward opens again what back closed, and any other
 * move of the back stack (a tap, a tab, a sheet, a swipe back) forgets what forward had. The web leaves this to the
 * browser's own history, and Android has no forward.
 */
class ForwardHistory(
    private val navigator: Navigator,
    scope: CoroutineScope,
) {
    // Why: what back closed, the last one on top, which forward opens again.
    private val closed = MutableStateFlow<List<Route>>(emptyList())

    // Why: the back stack as the last back or forward left it; any other one the back stack moves to forgets forward.
    private var left: List<Route>? = null

    val canGoForward: Flow<Boolean> = closed.map { it.isNotEmpty() }

    init {
        navigator.backStack
            .onEach { stack -> if (stack != left) forget() }
            .launchIn(scope)
    }

    /*
     * Why: the root never closes, and a back the navigator refused leaves forward as it was. Where back lands is set
     * before it moves, so the watch above, which may see the move at once, keeps what forward has.
     */
    fun goBack() {
        val stack = navigator.backStack.value
        if (stack.size < 2) return
        if (stack != left) forget()
        val kept = closed.value
        left = stack.dropLast(1)
        navigator.navigateBack()
        val now = navigator.backStack.value
        if (now == stack) {
            left = stack
            return
        }
        left = now
        closed.value = kept + stack.last()
    }

    fun goForward() {
        if (navigator.backStack.value != left) forget()
        val route = closed.value.lastOrNull() ?: return
        left = navigator.backStack.value + route
        navigator.navigate(route)
        closed.value = closed.value.dropLast(1)
    }

    private fun forget() {
        left = null
        closed.value = emptyList()
    }
}
