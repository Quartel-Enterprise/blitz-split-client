package com.quare.blitzsplit.feature.watchbalances.presentation.model

// Why: the word and the color each UI gives a balance: "to receive" in green, "you owe" in orange, or even.
enum class WatchBalanceDirection {
    TO_RECEIVE,
    TO_PAY,
    EVEN,
    ;

    companion object {
        fun of(netMinor: Long): WatchBalanceDirection = when {
            netMinor > 0 -> TO_RECEIVE
            netMinor < 0 -> TO_PAY
            else -> EVEN
        }
    }
}
