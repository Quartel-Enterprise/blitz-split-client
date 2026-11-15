package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.AppStores
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.support.domain.usecase.FindRateAppLink

/*
 * Why: the row opens the store's listing, never the in-app card, which the stores only show a few times a year and
 * may not show at all. Android is always on Google Play; the Apple apps wait for their App Store listing, whose page
 * opens at "Write a Review"; the web has no listing of its own and sends to the page that leads to the phone's store.
 */
class FindRateAppLinkUseCase(
    private val platform: ClientPlatform,
    private val stores: AppStores,
) : FindRateAppLink {
    override fun invoke(): String? = when (platform) {
        ClientPlatform.ANDROID -> GOOGLE_PLAY_LISTING
        ClientPlatform.IOS, ClientPlatform.MACOS -> stores.appStoreUrl?.let { "$it?action=write-review" }
        ClientPlatform.WEB -> AppInvite.WEB_LINK
    }

    private companion object {
        const val GOOGLE_PLAY_LISTING = "https://play.google.com/store/apps/details?id=com.quare.blitzsplit"
    }
}
