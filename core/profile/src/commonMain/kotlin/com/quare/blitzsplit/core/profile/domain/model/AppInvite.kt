package com.quare.blitzsplit.core.profile.domain.model

import com.quare.blitzsplit.core.model.StoreListing

/*
 * Why: "Get the app" (page 14) as this platform offers it: only the web and the Mac do, once a store lists the app.
 * Home's card, Profile's row (Settings on the Mac) and the app's code read the same state.
 */
data class AppInvite(
    // Why: what the QR code and the link open: blitzsplit.com/app-download on the web, the App Store on the Mac.
    val link: String,
    val stores: StoreListing,
    // Why: "I downloaded it" or ✕, on Home, Profile or the app's code, for good.
    val isDismissed: Boolean,
    val phoneUse: PhoneUse?,
) {
    // Why: Home's card and the highlighted row; after that the row stays, plain, as the way to download later.
    val isActive: Boolean
        get() = !isDismissed && phoneUse == null

    companion object {
        // Why: sends Android to Google Play, the iPhone to the App Store and anything else to a page with both.
        const val WEB_LINK: String = "https://blitzsplit.com/app-download"

        // Why: "blitzsplit.com/app-download", the link as the screens print it, without the language it carries.
        fun toDisplayLink(link: String): String = link.substringAfter("://").substringBefore("?")
    }
}
