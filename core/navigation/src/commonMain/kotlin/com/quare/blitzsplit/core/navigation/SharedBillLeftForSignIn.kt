package com.quare.blitzsplit.core.navigation

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/*
 * Why: "Entrar" on a bill's link page without a profile leaves the page for the welcome, which takes the whole stack.
 * The page says so here, since the stack alone can't tell signing in from going back, and the app opens it again once
 * the welcome puts the home in place.
 */
class SharedBillLeftForSignIn(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var left: Pair<Route.SharedBill, TimeMark>? = null

    fun leaveForWelcome(
        page: Route.SharedBill,
        navigator: Navigator,
    ) {
        left = page to timeSource.markNow()
        navigator.replaceAll(Route.Welcome)
    }

    // Why: read once, so a sign in given up on never opens the page on a later one; null once `window` has passed.
    fun take(window: Duration): Route.SharedBill? {
        val (page, since) = left ?: return null
        left = null
        return page.takeIf { since.elapsedNow() <= window }
    }
}
