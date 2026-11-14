package com.quare.blitzsplit.feature.contactsupport.presentation.model

import com.quare.blitzsplit.core.support.domain.model.SupportEmail

sealed interface ContactSupportUiAction {
    /*
     * Why: each platform hands its email app the message's mailto link, Android with the subject and the body apart
     * too; one that has no email app answers OnOpenEmailFailed.
     */
    data class OpenEmail(
        val message: SupportEmail,
    ) : ContactSupportUiAction

    // Why: the state's notice confirms it, so the UIs only write the clipboard; one refused answers OnCopyEmailFailed.
    data class CopyToClipboard(
        val email: String,
    ) : ContactSupportUiAction
}
