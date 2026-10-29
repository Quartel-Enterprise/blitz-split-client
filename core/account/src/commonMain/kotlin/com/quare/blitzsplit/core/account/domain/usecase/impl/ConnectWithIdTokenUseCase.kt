package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.model.AccountProvider

class ConnectWithIdTokenUseCase(
    private val repository: AccountRepository,
) : ConnectWithIdToken {
    override suspend fun invoke(
        provider: AccountProvider,
        idToken: String,
        rawNonce: String?,
        fullName: String?,
        authorizationCode: String?,
    ): Result<ConnectResult> = repository.signInWithIdToken(
        provider = provider,
        idToken = idToken,
        rawNonce = rawNonce,
        fullName = fullName,
        authorizationCode = authorizationCode,
    )
}
