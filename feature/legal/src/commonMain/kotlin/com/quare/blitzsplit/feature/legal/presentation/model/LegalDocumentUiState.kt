package com.quare.blitzsplit.feature.legal.presentation.model

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.model.LegalPage

data class LegalDocumentUiState(
    // Why: which text it is, for the screen's title before the text itself is there.
    val page: LegalPage,
    // Why: the text the device holds, null until it downloaded one.
    val document: LegalDocument?,
    // Why: the download failed; it only shows while there is no text, since a saved one is drawn instead.
    val hasLoadFailed: Boolean,
    /*
     * Why: the index in document.blocks of the section a link asked for (/privacy#keep), which the text opens scrolled
     * to; null for the top, and while the text isn't there or has no such section. The UIs scroll to it once, when the
     * text first shows: a newer text taking its place keeps the reader where they are.
     */
    val focusedBlockIndex: Int?,
) {
    val showsSkeleton: Boolean
        get() = document == null && !hasLoadFailed

    // Why: "Couldn't load", with "Try again": offline (or the site down) before the device ever saved this text.
    val showsLoadFailed: Boolean
        get() = document == null && hasLoadFailed
}
