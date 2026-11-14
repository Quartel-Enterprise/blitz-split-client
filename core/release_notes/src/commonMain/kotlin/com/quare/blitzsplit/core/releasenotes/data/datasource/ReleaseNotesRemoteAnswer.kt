package com.quare.blitzsplit.core.releasenotes.data.datasource

sealed interface ReleaseNotesRemoteAnswer {
    // Why: the feed's JSON, with the site's ETag for it (null when it sent none).
    data class Feed(
        val json: String,
        val etag: String?,
    ) : ReleaseNotesRemoteAnswer

    // Why: 304, the feed whose ETag the app sent is still the current one.
    data object Unchanged : ReleaseNotesRemoteAnswer

    // Why: 404, the site doesn't write the release notes in that language.
    data object Missing : ReleaseNotesRemoteAnswer
}
