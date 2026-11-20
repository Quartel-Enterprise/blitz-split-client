package com.quare.blitzsplit.feature.watchpairing.presentation.model

// Why: which of the boards 15.9 to 15.13 the watch shows.
enum class WatchPairingStep {
    // Why: 15.9, the first launch, with "Connect".
    WELCOME,

    // Why: 15.10, the code on its way to the phone, which approves it on its own.
    CONNECTING,

    // Why: 15.11c, no phone answered: "Try again" or "Use code".
    PHONE_OUT_OF_REACH,

    // Why: 15.11b, the phone has a local only profile, with no account to share.
    LOCAL_ONLY,

    // Why: 15.12, the code shown, typed in Profile › Connect watch on any device with the account.
    CODE,

    // Why: 15.11, signed in as the phone's user, with "Continue".
    CONNECTED,

    // Why: 15.13, the phone signed out, so the watch did too and erased its data.
    SIGNED_OUT_ON_PHONE,
}
