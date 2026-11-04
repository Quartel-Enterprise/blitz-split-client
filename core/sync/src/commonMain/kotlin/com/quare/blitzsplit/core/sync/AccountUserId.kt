package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.model.AccountStatus

// Why: the server's user of the account signed in, none while the install only lives on the device.
internal fun userIdOrNull(status: AccountStatus): String? = when (status) {
    AccountStatus.LocalOnly -> null
    is AccountStatus.Anonymous -> status.userId
    is AccountStatus.Connected -> status.userId
}
