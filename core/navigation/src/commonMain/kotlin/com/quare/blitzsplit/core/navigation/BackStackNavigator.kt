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

    // Why: a double tap would push the same screen twice, and Navigation 3 keys each entry by its route.
    override fun navigate(route: Route) {
        entries.update { stack -> if (stack.lastOrNull() == route) stack else stack + route }
    }

    override fun navigateBack() {
        entries.update { stack -> if (stack.size > 1) stack.dropLast(1) else stack }
    }

    override fun replaceAll(route: Route) {
        entries.value = listOf(route)
    }

    override fun popTo(size: Int) {
        entries.update { stack -> stack.take(size.coerceIn(1, stack.size)) }
    }
}
