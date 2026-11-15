package com.quare.blitzsplit.core.provider.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/*
 * Why: the release notes the site last answered for a language, as the JSON it sent, so "Release notes" opens offline
 * and at once. language is the one asked for ("fr"), which the site may have answered in English. Kept as it came and
 * read again each time it is drawn, so an app that learns more of the feed shows it without a download.
 */
@Entity(tableName = "release_notes_feed")
data class ReleaseNotesFeedEntity(
    @PrimaryKey
    val language: String,
    val json: String,
    // Why: the site's ETag for this feed, sent back so an unchanged one answers 304 with no body; null without one.
    val etag: String?,
    // Why: when the site last confirmed this feed (epoch millis).
    val checkedAt: Long,
    // Why: the app's version ("3.1.0-beta_2") when this feed was checked, null for one whose name can't be read.
    val installedVersion: String?,
    // Why: when this feed was first checked on that version (epoch millis), which bounds its quicker checks.
    val installedVersionSeenAt: Long?,
)
