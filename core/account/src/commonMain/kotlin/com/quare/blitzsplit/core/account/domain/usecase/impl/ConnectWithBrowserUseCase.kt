package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.model.AccountProvider

/*
 * Why: the screen that starts the link says whether it is the profile, rather than the top of the back stack: the
 * Mac's profile lives in Ajustes, its own window, never on the back stack.
 */
class ConnectWithBrowserUseCase(
    private val repository: AccountRepository,
) : ConnectWithBrowser {
    override suspend fun invoke(
        provider: AccountProvider,
        isFromProfile: Boolean,
    ): Result<Unit> = repository.signInWithBrowser(
        provider = provider,
        isFromProfile = isFromProfile,
    )
}
