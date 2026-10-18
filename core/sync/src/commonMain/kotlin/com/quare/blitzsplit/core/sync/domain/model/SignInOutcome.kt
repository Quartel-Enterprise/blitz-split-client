package com.quare.blitzsplit.core.sync.domain.model

enum class SignInOutcome {
    // Why: a returning user whose profile came back from the server goes straight to the groups.
    PROFILE_RESTORED,
    NEEDS_PROFILE,
}
