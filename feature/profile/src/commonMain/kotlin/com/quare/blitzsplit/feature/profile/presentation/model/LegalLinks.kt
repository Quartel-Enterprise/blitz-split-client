package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.LegalPage

// Why: the site's pages, which the web opens in the browser; the other platforms draw the same texts themselves.
internal object LegalLinks {
    fun urlOf(page: LegalPage): String = "https://blitzsplit.com/${page.slug}"
}
