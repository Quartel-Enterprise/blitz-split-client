package com.quare.blitzsplit.core.model

// Why: why this device's "Apagar grupo", taken off the screens at once, didn't go through on the server.
enum class GroupDeleteFailure {
    // Why: a bill or payment the device didn't have yet leaves someone owing; the sync just brought it.
    HAS_BALANCE,

    // Why: the server couldn't be reached or failed; nothing changed there.
    FAILED,
}
