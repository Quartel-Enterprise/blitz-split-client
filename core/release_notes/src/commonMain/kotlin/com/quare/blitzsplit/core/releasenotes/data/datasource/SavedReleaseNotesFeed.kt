package com.quare.blitzsplit.core.releasenotes.data.datasource

// Why: what the device knows of the feed it saved for a language, to ask the site only when it may be stale.
data class SavedReleaseNotesFeed(
    // Why: the site's ETag for it, null when the site sent none.
    val etag: String?,
    // Why: when the site last confirmed it (epoch millis).
    val checkedAt: Long,
)
