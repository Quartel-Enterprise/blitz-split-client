package com.quare.blitzsplit.feature.legal.presentation.model

sealed interface LegalDocumentUiAction {
    // Why: a link of the text that leaves the app: a web page in the browser, an address in the mail app.
    data class OpenUrl(
        val url: String,
    ) : LegalDocumentUiAction
}
