package com.quare.blitzsplit.core.legal.domain.model

import com.quare.blitzsplit.core.model.LegalPage

sealed interface LegalLink {
    // Why: an https or mailto address, which the platform opens.
    data class External(
        val url: String,
    ) : LegalLink

    // Why: the other legal text, or a section of one, which opens in the app.
    data class Page(
        val page: LegalPage,
        val anchor: String?,
    ) : LegalLink
}
