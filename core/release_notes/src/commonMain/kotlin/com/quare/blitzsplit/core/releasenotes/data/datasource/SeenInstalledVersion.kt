package com.quare.blitzsplit.core.releasenotes.data.datasource

/*
 * Why: the app's version when a saved feed was checked ("3.1.0-beta_2") and when the feed was first checked on it
 * (epoch millis): a feed without that version is asked again sooner only in its first hours.
 */
data class SeenInstalledVersion(
    val name: String,
    val seenAt: Long,
)
