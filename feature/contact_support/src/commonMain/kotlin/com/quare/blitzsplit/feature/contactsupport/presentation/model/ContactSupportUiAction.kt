package com.quare.blitzsplit.feature.contactsupport.presentation.model

sealed interface ContactSupportUiAction {
    // Why: a mailto link, which each platform hands its email app; one that has none answers OnOpenEmailFailed.
    data class OpenEmail(
        val mailtoLink: String,
    ) : ContactSupportUiAction

    // Why: the state's notice confirms it, so the UIs only write the clipboard.
    data class CopyToClipboard(
        val email: String,
    ) : ContactSupportUiAction
}
