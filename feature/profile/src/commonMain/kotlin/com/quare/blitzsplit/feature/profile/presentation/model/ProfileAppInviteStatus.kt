package com.quare.blitzsplit.feature.profile.presentation.model

// Why: what "App on your phone" says (14.4): highlighted, plain for good, or already in use on a phone.
enum class ProfileAppInviteStatus {
    // Why: highlighted, with the app's icon, the main button and ✕ (14.4).
    ACTIVE,

    // Why: after "I downloaded it" or ✕, plain for good (14.4b).
    DISMISSED,

    // Why: the account signed in on a phone app: "You already use the app on your phone" (14.4c).
    IN_USE,
}
