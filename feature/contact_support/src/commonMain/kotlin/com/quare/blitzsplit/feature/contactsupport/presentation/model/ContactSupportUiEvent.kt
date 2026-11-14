package com.quare.blitzsplit.feature.contactsupport.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ContactSupportUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: only a dismissal that closes the sheet, never a second one sent during its exit.
            OnDismissed -> EventAnalytics.Manual("contact_support_dismissed")

            // Why: only once the diagnostics are read, since the email carries them.
            OnOpenEmailClicked -> EventAnalytics.Manual("contact_support_open_email_clicked")

            OnCopyEmailClicked -> EventAnalytics.Automatic("contact_support_copy_email_clicked")

            // Why: the platform's answer, which ends what the tap on "Open email" began.
            OnOpenEmailFailed -> EventAnalytics.Automatic("contact_support_no_email_app")
        }

    // Why: the back arrow, Close and a native dismiss of the sheet or dialog.
    data object OnDismissed : ContactSupportUiEvent

    data object OnOpenEmailClicked : ContactSupportUiEvent

    data object OnCopyEmailClicked : ContactSupportUiEvent

    // Why: no app on the device takes a mailto link (Android and Apple say so; a browser never does).
    data object OnOpenEmailFailed : ContactSupportUiEvent
}
