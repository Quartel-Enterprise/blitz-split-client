package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackStackNavigator(
    startRoute: Route,
) : Navigator {
    private val entries = MutableStateFlow(listOf(startRoute))

    override val backStack: StateFlow<List<Route>> = entries.asStateFlow()

    /*
     * Why: Navigation 3 keys each entry by its route, and each screen asks whether its route is on top, so the same
     * route never sits twice on the stack: a double tap pushes nothing, and a route already lower in the stack (a bill
     * opened again from its group opened over its own form) goes back to it, closing what is above it.
     */
    override fun navigate(route: Route) {
        entries.update { stack ->
            val index = stack.indexOf(route)
            if (index >= 0) stack.take(index + 1) else stack + route
        }
    }

    override fun navigateBack() {
        entries.update { stack -> if (stack.size > 1) stack.dropLast(1) else stack }
    }

    override fun replaceTop(route: Route) {
        entries.update { stack -> stack.dropLast(1) + route }
    }

    override fun replaceAll(route: Route) {
        entries.value = listOf(route)
    }

    override fun replaceAbove(
        size: Int,
        route: Route,
    ) {
        replaceAbove(size, listOf(route))
    }

    override fun replaceAbove(
        size: Int,
        routes: List<Route>,
    ) {
        entries.update { stack -> stack.take(size.coerceIn(1, stack.size)) + routes }
    }

    override fun popTo(size: Int) {
        entries.update { stack -> stack.take(size.coerceIn(1, stack.size)) }
    }
}
