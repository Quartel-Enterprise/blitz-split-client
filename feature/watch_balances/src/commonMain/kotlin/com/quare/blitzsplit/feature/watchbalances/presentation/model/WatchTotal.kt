package com.quare.blitzsplit.feature.watchbalances.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: "Overall balance" in one currency (15.3), and the Tile's and complications' figures.
data class WatchTotal(
    val currency: GroupCurrency,
    // Why: positive when you are owed on the whole ("to receive"), negative when you owe.
    val netMinor: Long,
    val toReceiveMinor: Long,
    // Why: positive, what you owe; each UI writes its minus.
    val toPayMinor: Long,
) {
    val direction: WatchBalanceDirection
        get() = WatchBalanceDirection.of(netMinor)
}
