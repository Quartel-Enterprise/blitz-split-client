package com.quare.blitzsplit.feature.watchpairing.presentation.model

// Why: why the welcome's Connect didn't start or didn't end; each UI writes it.
enum class WatchPairingError {
    // Why: the watch has no connection, which asking for a code and signing in both need.
    NO_INTERNET,

    // Why: the server didn't give a code, or the sign in it approved failed.
    FAILED,
}
