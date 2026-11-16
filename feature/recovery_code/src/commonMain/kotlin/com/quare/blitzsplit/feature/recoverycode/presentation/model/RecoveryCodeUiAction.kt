package com.quare.blitzsplit.feature.recoverycode.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider

sealed interface RecoveryCodeUiAction {
    // Why: the words joined by single spaces, which the restore screen takes back pasted whole.
    data class CopyToClipboard(
        val code: String,
    ) : RecoveryCodeUiAction

    // Why: a password manager entry on Android and Apple; on the web it becomes a file download.
    data class SaveToPasswordManager(
        val code: String,
    ) : RecoveryCodeUiAction

    data class LaunchNativeSignIn(
        val provider: AccountProvider,
    ) : RecoveryCodeUiAction
}
