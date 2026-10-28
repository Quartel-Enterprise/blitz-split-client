package com.quare.blitzsplit.core.account.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route

// Why: the recovery code sheet links from the groups or from the profile, which only the back stack tells.
class ConnectWithBrowserUseCase(
    private val repository: AccountRepository,
    private val navigator: Navigator,
) : ConnectWithBrowser {
    override suspend fun invoke(provider: AccountProvider): Result<Unit> = repository.signInWithBrowser(
        provider = provider,
        isFromProfile = navigator.backStack.value.lastOrNull() == Route.Profile,
    )
}
