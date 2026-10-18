package com.quare.blitzsplit.feature.recoverycode.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface RecoveryCodeUiAction {
    data class CopyToClipboard(
        val formattedCode: String,
    ) : RecoveryCodeUiAction

    // Why: a password manager entry on Android and Apple; on the web it becomes a file download.
    data class SaveToPasswordManager(
        val formattedCode: String,
    ) : RecoveryCodeUiAction

    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : RecoveryCodeUiAction
}
