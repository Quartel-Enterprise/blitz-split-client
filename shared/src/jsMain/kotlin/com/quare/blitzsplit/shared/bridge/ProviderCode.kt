package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.AccountProvider

internal fun String.toAccountProvider(): AccountProvider = requireNotNull(AccountProvider.fromCodeOrNull(this)) {
    "Unknown provider $this"
}
