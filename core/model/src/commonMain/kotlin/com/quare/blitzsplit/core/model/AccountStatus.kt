package com.quare.blitzsplit.core.model

sealed interface AccountStatus {
    // Why: the first connection has not happened yet, so the server doesn't know this install.
    data object LocalOnly : AccountStatus

    data class Anonymous(
        val userId: String,
    ) : AccountStatus

    data class Connected(
        val userId: String,
        val provider: AccountProvider,
    ) : AccountStatus
}
