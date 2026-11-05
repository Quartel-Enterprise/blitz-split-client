package com.quare.blitzsplit.core.navigation

/*
 * Why: a screen's back pops only while that screen is still on top. A second tap during the exit animation, or a
 * dismissal sent twice (a backdrop and a close button), would otherwise close the screen under it too.
 */
fun Navigator.navigateBackFrom(route: Route) {
    if (backStack.value.lastOrNull() == route) navigateBack()
}
