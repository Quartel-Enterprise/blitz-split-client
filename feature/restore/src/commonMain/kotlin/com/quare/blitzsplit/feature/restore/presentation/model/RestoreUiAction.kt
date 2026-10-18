package com.quare.blitzsplit.feature.restore.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface RestoreUiAction {
    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : RestoreUiAction

    data object OpenQrScanner : RestoreUiAction
}
