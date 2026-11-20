package com.quare.blitzsplit.feature.watchbalances.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal

/*
 * Why: what a watch's Tile and complications show (15.1, 15.2), so every watch's glances decide it as Balances does: a
 * watch with no account yet (never connected, or signed out) asks to connect rather than read "All settled"; else the
 * total Balances puts on top, the first open one, in the currency with the most groups.
 */
sealed interface WatchGlance {
    data object NotConnected : WatchGlance

    data object Settled : WatchGlance

    data class Open(
        val total: WatchTotal,
    ) : WatchGlance

    companion object {
        fun of(
            hasProfile: Boolean,
            balances: PeopleBalances,
        ): WatchGlance {
            if (!hasProfile) return NotConnected
            return WatchBalancesMapper.totalsOf(balances).firstOrNull()?.let(::Open) ?: Settled
        }
    }
}
