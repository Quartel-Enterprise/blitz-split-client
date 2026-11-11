package com.quare.blitzsplit.feature.contacts.presentation.model

enum class ProfileLinkStage {
    // Why: asking the server whose link it is.
    LOADING,

    // Why: the server didn't answer (offline, most of the time), with "Try again"; it asks again once back online.
    UNREACHABLE,

    // Why: no profile has the code: a mistyped link, or an account deleted since.
    NOT_FOUND,

    // Why: someone this device shares no group with: their name and photo, with the invite to a group.
    STRANGER,
}
