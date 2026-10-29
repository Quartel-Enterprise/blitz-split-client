package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.model.AccountProvider

fun interface ConnectWithIdToken {
    suspend operator fun invoke(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
        // Why: only Apple's native sheet gives it, for the server to keep Apple's token.
        authorizationCode: String?,
    ): Result<ConnectResult>
}
