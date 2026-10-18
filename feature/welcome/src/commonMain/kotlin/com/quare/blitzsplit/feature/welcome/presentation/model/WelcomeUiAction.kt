package com.quare.blitzsplit.feature.welcome.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface WelcomeUiAction {
    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : WelcomeUiAction
}
