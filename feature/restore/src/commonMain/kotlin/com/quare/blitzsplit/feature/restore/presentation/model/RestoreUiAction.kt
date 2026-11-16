package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface RestoreUiAction {
    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : RestoreUiAction

    data object OpenQrScanner : RestoreUiAction

    // Why: a word completed or picked moves the keyboard to the next field.
    data class FocusWord(
        val index: Int,
    ) : RestoreUiAction
}
