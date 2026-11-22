package com.quare.blitzsplit.shared.push

enum class PushArrivalOutcome {
    // Why: iOS's fetch handler tells "no data" apart, which its scheduling of later wakes weighs.
    NOTHING_NEW,
    SYNCED,
    FAILED,
}
