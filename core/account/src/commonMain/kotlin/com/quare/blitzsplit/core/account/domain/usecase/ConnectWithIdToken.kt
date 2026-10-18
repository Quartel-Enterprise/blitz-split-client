package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.model.AccountProvider

fun interface ConnectWithIdToken {
    suspend operator fun invoke(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
    ): Result<ConnectResult>
}
