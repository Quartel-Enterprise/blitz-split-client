package com.quare.blitzsplit.feature.contactsupport.presentation.model

import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded

data class ContactSupportUiState(
    // Why: the address under "To", which Copy takes and the email goes to.
    val email: String,
    // Why: "Will be included in the email"; loading until the account, the language and the profile's link were read.
    val diagnostics: Loadable<SupportDiagnostics>,
    // Why: what the last tap did, until "Open email" is tried again; null before any.
    val notice: ContactSupportNotice?,
) {
    // Why: the email carries the diagnostics, so it waits for them.
    val canOpenEmail: Boolean
        get() = diagnostics.isLoaded()
}
