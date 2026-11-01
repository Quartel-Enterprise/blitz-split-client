package com.quare.blitzsplit.core.activity.domain.usecase.impl

import com.quare.blitzsplit.core.model.AccountStatus

// Why: the account of this device, whose own events are read from the start; none before its first sync.
internal fun AccountStatus.userIdOrNull(): String? = when (this) {
    AccountStatus.LocalOnly -> null
    is AccountStatus.Anonymous -> userId
    is AccountStatus.Connected -> userId
}
