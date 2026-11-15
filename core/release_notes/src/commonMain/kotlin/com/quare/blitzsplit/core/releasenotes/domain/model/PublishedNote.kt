package com.quare.blitzsplit.core.releasenotes.domain.model

data class PublishedNote(
    val text: String,
    // Why: the feed's own names ("android", "ios", "mac", "web"), or "all" alone for every platform.
    val platforms: Set<String>,
)
