package com.quare.blitzsplit.feature.watchpairing.presentation.model

// Why: what the phone said back about the code the watch sent it, over the Data Layer or WatchConnectivity.
enum class PhoneReply {
    // Why: the phone approved it; the watch's poll brings the session.
    APPROVED,

    // Why: the phone's profile is local only (15.11b).
    LOCAL_ONLY,

    // Why: no phone took the message, or the one that did couldn't approve it.
    UNREACHABLE,
}
