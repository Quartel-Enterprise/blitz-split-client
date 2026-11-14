package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.LegalPage

/*
 * Why: Profile's footer and Welcome's notice open the legal texts the same way: the app draws them itself on Android,
 * iOS and the Mac, while a browser opens the site's page in another tab, which openUrl hands to the web.
 */
fun interface OpenLegalPage {
    operator fun invoke(
        page: LegalPage,
        openUrl: (String) -> Unit,
    )
}
