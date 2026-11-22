package com.quare.blitzsplit.feature.connectwatch.presentation.model

// Why: why Connect didn't connect the watch; each UI writes it.
enum class ConnectWatchError {
    // Why: 15.14b "Wrong or expired code. Get another one on the watch."
    INVALID_OR_EXPIRED,

    // Why: too many wrong codes in a row; the server takes no more for a while.
    TOO_MANY_ATTEMPTS,

    // Why: a local only profile has no account for the watch to use.
    NEEDS_ACCOUNT,

    // Why: the server didn't answer.
    FAILED,
}
