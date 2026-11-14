package com.quare.blitzsplit.feature.appdownload.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.profile.domain.model.AppInvite

/*
 * Why: "Bora Rachar on your phone" (14.2): the QR code of the app's link, the stores it names, and, once the account
 * signed in on a phone app, since when (14.2b). The QR code is encoded here, so it shows offline too.
 */
data class AppDownloadUiState(
    val isLoaded: Boolean,
    // Why: what the code opens: blitzsplit.com/app-download on the web, the App Store's listing on the Mac.
    val link: String,
    val qrCode: QrCode?,
    val stores: StoreListing,
    // Why: "You've used the app on your phone since Oct 12" (14.2b), the server's clock in epoch millis.
    val phoneUseSinceMillis: Long?,
    // Why: "Sign in to the app with the same Google account"; null without one, whose phone app starts empty.
    val accountProvider: AccountProvider?,
    val isDismissed: Boolean,
) {
    // Why: the link as the code prints it under itself.
    val displayLink: String
        get() = AppInvite.toDisplayLink(link)

    val usesPhone: Boolean
        get() = phoneUseSinceMillis != null

    // Why: "I downloaded it" while the invite still stands; after it, or with the app in use, only Close.
    val showsDownloadedButton: Boolean
        get() = isLoaded && !isDismissed && !usesPhone
}
