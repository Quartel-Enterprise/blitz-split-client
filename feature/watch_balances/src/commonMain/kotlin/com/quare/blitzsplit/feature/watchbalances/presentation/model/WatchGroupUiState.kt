package com.quare.blitzsplit.feature.watchbalances.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

data class WatchGroupUiState(
    val isLoaded: Boolean,
    val groupName: String?,
    val currency: GroupCurrency,
    // Why: your net in the group, in big on top (15.4).
    val netMinor: Long,
    // Why: each pair with you, the largest first.
    val debts: List<WatchDebt>,
    // Why: the pair whose "I paid" asks "Paid R$ 98.00 to Carla?" (15.5); null while nothing is asked.
    val confirming: WatchDebt?,
    val isRecording: Boolean,
    // Why: a payment of the group recorded here waits to go up ("waiting to send").
    val isQueued: Boolean,
) {
    /*
     * Why: the EdgeButton "I paid" names no one, so it shows only while you owe a single person; with two or more,
     * each row you owe on is its own "I paid".
     */
    val singleDebtToPay: WatchDebt?
        get() = debts.singleOrNull(WatchDebt::isOwedByMe)

    val showsIPaid: Boolean
        get() = singleDebtToPay != null && !isRecording

    // Why: with a single pair, the net's card says whom ("you owe Carla") and no row repeats it (15.4).
    val cardDebt: WatchDebt?
        get() = debts.singleOrNull()

    // Why: the rows under the net's card, one per pair once there are two or more.
    val rowDebts: List<WatchDebt>
        get() = if (debts.size > 1) debts else emptyList()

    val areDebtsClickable: Boolean
        get() = debts.count(WatchDebt::isOwedByMe) > 1 && !isRecording
}
