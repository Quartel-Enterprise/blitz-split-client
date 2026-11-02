package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.model.AccountProvider

fun interface ConnectWithBrowser {
    suspend operator fun invoke(
        provider: AccountProvider,
        isFromProfile: Boolean,
    ): Result<Unit>
}
