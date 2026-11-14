package com.quare.blitzsplit.feature.welcome.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface WelcomeUiAction {
    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : WelcomeUiAction

    // Why: the web opens the legal texts on the site, in another tab, as a browser does.
    data class OpenUrl(
        val url: String,
    ) : WelcomeUiAction
}
