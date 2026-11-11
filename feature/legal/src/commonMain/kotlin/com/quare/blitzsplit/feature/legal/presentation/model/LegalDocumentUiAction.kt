package com.quare.blitzsplit.feature.legal.presentation.model

sealed interface LegalDocumentUiAction {
    // Why: a link of the text that leaves the app: a web page in the browser, an address in the mail app.
    data class OpenUrl(
        val url: String,
    ) : LegalDocumentUiAction

    /*
     * Why: a link to the section this very screen was opened at: the text scrolls back to it, the index in
     * document.blocks, or to its top for null (no section asked for, or one the text doesn't have).
     */
    data class ScrollToSection(
        val blockIndex: Int?,
    ) : LegalDocumentUiAction
}
