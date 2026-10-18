package com.quare.blitzsplit.core.account.domain.model

enum class ConnectResult {
    // Why: the anonymous user got the identity, so its id and every row stay the same.
    LINKED,

    // Why: the identity already belonged to another user, signed in on another device before.
    SWITCHED,
    SIGNED_IN,
}
