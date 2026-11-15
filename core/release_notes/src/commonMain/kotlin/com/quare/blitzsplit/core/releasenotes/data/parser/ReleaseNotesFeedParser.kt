package com.quare.blitzsplit.core.releasenotes.data.parser

import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease

fun interface ReleaseNotesFeedParser {
    // Why: null for anything that isn't the feed (an error page a proxy answered with), so it never replaces a saved one.
    fun parse(json: String): List<PublishedRelease>?
}
