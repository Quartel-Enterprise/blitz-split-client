package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.QrCode
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.profile.domain.model.AppInvite

/*
 * Why: "Take Bora Rachar on your phone" (14.1), the web's and the Mac's card: the QR code of the app's link, which
 * opens the stores, and the stores its copy names. Encoded here, so the card shows offline too.
 */
data class HomeAppInvite(
    val link: String,
    val qrCode: QrCode,
    val stores: StoreListing,
    // Why: neither dismissed nor already in use on a phone; only then does the card come.
    val isActive: Boolean,
) {
    // Why: the link as the card prints it.
    val displayLink: String
        get() = AppInvite.toDisplayLink(link)
}
