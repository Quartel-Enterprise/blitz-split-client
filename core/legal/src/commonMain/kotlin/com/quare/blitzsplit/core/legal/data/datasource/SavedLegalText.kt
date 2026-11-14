package com.quare.blitzsplit.core.legal.data.datasource

// Why: what the device knows of the text it saved for a page and language, to ask the site only when it may be stale.
data class SavedLegalText(
    // Why: the site's ETag for it, null when the site sent none or the text was saved before the app kept it.
    val etag: String?,
    // Why: when the site last confirmed it (epoch millis), null for a text saved before the app kept it.
    val checkedAt: Long?,
)
