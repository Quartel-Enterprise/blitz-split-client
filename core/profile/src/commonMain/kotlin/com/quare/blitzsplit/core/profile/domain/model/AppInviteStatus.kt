package com.quare.blitzsplit.core.profile.domain.model

// Why: what the device knows of the invite, wherever it shows: dismissed for good, and whether a phone app is in use.
data class AppInviteStatus(
    val isDismissed: Boolean,
    val phoneUse: PhoneUse?,
)
