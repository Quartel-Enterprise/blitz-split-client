package com.quare.blitzsplit.core.model

// Why: the server's user of the account signed in, none while the install only lives on the device.
fun AccountStatus.userIdOrNull(): String? = when (this) {
    AccountStatus.LocalOnly -> null
    is AccountStatus.Anonymous -> userId
    is AccountStatus.Connected -> userId
}
