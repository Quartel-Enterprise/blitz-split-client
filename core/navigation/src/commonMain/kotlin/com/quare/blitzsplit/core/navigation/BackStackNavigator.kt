package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackStackNavigator(
    startRoute: Route,
) : Navigator {
    private val entries = MutableStateFlow(listOf(startRoute))

    override val backStack: StateFlow<List<Route>> = entries.asStateFlow()

    private val unsaved = mutableSetOf<Route>()

    private val anyUnsaved = MutableStateFlow(false)

    override val hasUnsavedInput: StateFlow<Boolean> = anyUnsaved.asStateFlow()

    // Why: the stack the held change would leave, applied only when "Discard" is picked.
    private var held: List<Route>? = null

    private val discardPending = MutableStateFlow(false)

    override val pendingDiscard: StateFlow<Boolean> = discardPending.asStateFlow()

    override fun markUnsaved(
        route: Route,
        hasUnsavedInput: Boolean,
    ) {
        if (hasUnsavedInput && route in entries.value) unsaved += route else unsaved -= route
        anyUnsaved.value = unsaved.isNotEmpty()
        // Why: a form that became clean (saved, its group gone) leaves nothing to ask about the change held for it.
        val next = held
        if (next != null && entries.value.none { it in unsaved && it !in next }) cancelDiscard()
    }

    override fun confirmDiscard() {
        val next = held ?: return
        held = null
        discardPending.value = false
        commit(next)
    }

    override fun cancelDiscard() {
        held = null
        discardPending.value = false
    }

    /*
     * Why: every change goes through here. One that would close a route with unsaved input is held for the answer;
     * a later change replaces the one held, since the screen the person is on is what they asked for last.
     */
    private fun change(transform: (List<Route>) -> List<Route>) {
        val next = transform(entries.value)
        if (entries.value.any { it in unsaved && it !in next }) {
            held = next
            discardPending.value = true
        } else {
            cancelDiscard()
            commit(next)
        }
    }

    private fun commit(next: List<Route>) {
        entries.value = next
        unsaved.retainAll(next.toSet())
        anyUnsaved.value = unsaved.isNotEmpty()
    }

    /*
     * Why: Navigation 3 keys each entry by its route, and each screen asks whether its route is on top, so the same
     * route never sits twice on the stack: a double tap pushes nothing, and a route already lower in the stack (a bill
     * opened again from its group opened over its own form) goes back to it, closing what is above it.
     */
    override fun navigate(route: Route) {
        change { stack ->
            val index = stack.indexOf(route)
            if (index >= 0) stack.take(index + 1) else stack + route
        }
    }

    override fun navigateBack() {
        change { stack -> if (stack.size > 1) stack.dropLast(1) else stack }
    }

    override fun replaceTop(route: Route) {
        change { stack -> stack.dropLast(1) + route }
    }

    override fun replaceAll(route: Route) {
        // Why: it starts the app over (a profile restored, an account deleted), so nothing is left to ask about.
        cancelDiscard()
        unsaved.clear()
        anyUnsaved.value = false
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
        change { stack -> stack.take(size.coerceIn(1, stack.size)) + routes }
    }

    override fun popTo(size: Int) {
        change { stack -> stack.take(size.coerceIn(1, stack.size)) }
    }
}
