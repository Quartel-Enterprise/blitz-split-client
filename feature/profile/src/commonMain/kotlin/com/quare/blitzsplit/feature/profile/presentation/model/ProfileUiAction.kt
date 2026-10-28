package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface ProfileUiAction {
    data class CopyToClipboard(
        val formattedCode: String,
    ) : ProfileUiAction

    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : ProfileUiAction

    // Why: each platform opens the page its own way: a Custom Tab, Safari, or a new browser tab.
    data class OpenUrl(
        val url: String,
    ) : ProfileUiAction
}
