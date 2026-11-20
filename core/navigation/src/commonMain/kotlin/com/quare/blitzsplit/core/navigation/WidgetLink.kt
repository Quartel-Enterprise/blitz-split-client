package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: what a tap on the balance widget opens (the iPhone's Home Screen and Lock Screen, later the watch's
 * complication), borarachar://balances: the Balances tab, over wherever the app is.
 */
object WidgetLink {
    const val BALANCES = "borarachar://balances"

    // Why: null for any other URL, which the other links handle.
    fun homeTabOf(url: String): HomeTab? = HomeTab.BALANCES.takeIf { url.trim().trimEnd('/') == BALANCES }
}
