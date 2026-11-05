package com.quare.blitzsplit.core.profile.domain.model

// Why: the account signed in on the Android or iPhone app: "You've used the app on your phone since Oct 12".
data class PhoneUse(
    val platforms: Set<ClientPlatform>,
    // Why: the first sign in on any of them, the server's clock in epoch millis.
    val sinceMillis: Long,
)
