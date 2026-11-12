package com.quare.blitzsplit.core.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
 * Why: a screen's back pops only while that screen is still on top. A second tap during the exit animation, or a
 * dismissal sent twice (a backdrop and a close button), would otherwise close the screen under it too.
 */
fun Navigator.navigateBackFrom(route: Route) {
    if (backStack.value.lastOrNull() == route) navigateBack()
}

/*
 * Why: a form tells the back stack, for as long as its ViewModel lives, whether it holds input the person would lose
 * by leaving, so every way out asks "Discard changes?" first (see Navigator.markUnsaved).
 */
fun Navigator.guardUnsavedInput(
    scope: CoroutineScope,
    route: Route,
    hasUnsavedInput: Flow<Boolean>,
) {
    hasUnsavedInput
        .distinctUntilChanged()
        .onEach { markUnsaved(route, it) }
        .launchIn(scope)
}
