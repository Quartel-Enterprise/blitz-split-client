package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter

/*
 * Why: what a tap on the balance widget opens (the iPhone's Home Screen and Lock Screen, later the watch's
 * complication), borarachar://balances: Balances on All, the overall balance the widget shows, never a filter a
 * Balances left open in the back stack had.
 */
object WidgetLink {
    const val BALANCES = "borarachar://balances"

    // Why: null for any other URL, which the other links handle.
    fun routeOf(url: String): Route? = Route.GlobalBalances(BalancesFilter.ALL).takeIf {
        url.trim().trimEnd('/') ==
            BALANCES
    }
}
