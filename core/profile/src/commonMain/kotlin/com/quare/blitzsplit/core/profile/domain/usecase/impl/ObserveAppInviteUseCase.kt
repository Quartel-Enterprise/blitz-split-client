package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.LinkLanguage
import com.quare.blitzsplit.core.model.StoreListing
import com.quare.blitzsplit.core.profile.domain.model.AppInvite
import com.quare.blitzsplit.core.profile.domain.model.AppStores
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveAppInvite
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveSpokenLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class ObserveAppInviteUseCase(
    private val repository: AppInviteRepository,
    private val platform: ClientPlatform,
    private val stores: AppStores,
    private val observeSpokenLanguage: ObserveSpokenLanguage,
) : ObserveAppInvite {
    override fun invoke(): Flow<AppInvite?> {
        val (link, listing) = findOffer() ?: return flowOf(null)
        return combine(repository.observeStatus(), observeSpokenLanguage()) { status, language ->
            AppInvite(
                // Why: the site's link says the app's language, for its preview and its page; a store's own doesn't.
                link = if (platform == ClientPlatform.WEB) LinkLanguage.buildLink(link, language) else link,
                stores = listing,
                isDismissed = status.isDismissed,
                phoneUse = status.phoneUse,
            )
        }
    }

    /*
     * Why: the web names the stores that have the app; the Mac, sold through the Mac App Store, only ever points to
     * the App Store and never cites Android (guideline 2.3.10), so it waits for an App Store listing.
     */
    private fun findOffer(): Pair<String, StoreListing>? = when (platform) {
        ClientPlatform.WEB -> findWebListing()?.let { AppInvite.WEB_LINK to it }
        ClientPlatform.MACOS -> stores.appStoreUrl?.let { it to StoreListing.APP_STORE }
        ClientPlatform.ANDROID, ClientPlatform.IOS -> null
    }

    private fun findWebListing(): StoreListing? {
        val hasAppStore = stores.appStoreId != null
        return when {
            stores.googlePlay && hasAppStore -> StoreListing.BOTH
            stores.googlePlay -> StoreListing.GOOGLE_PLAY
            hasAppStore -> StoreListing.APP_STORE
            else -> null
        }
    }
}
