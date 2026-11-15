package com.quare.blitzsplit.core.releasenotes

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedNote
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate

// Why: the tests of this module share the feed as the site writes it and as the app reads it; the defaults match.
internal fun feedJson(vararg releases: String): String = """{"releases":[${releases.joinToString(",")}]}"""

internal fun releaseJson(
    version: String,
    date: String = "2026-11-20",
    notes: List<String> = listOf(noteJson()),
): String {
    val beta = "beta" in version
    return """{"version":"$version","beta":$beta,"date":"$date","notes":[${notes.joinToString(",")}]}"""
}

internal fun noteJson(
    text: String = "Faster sync",
    platforms: List<String> = listOf("all"),
): String = """{"text":"$text","platforms":[${platforms.joinToString(",") { "\"$it\"" }}]}"""

internal fun version(name: String): AppVersion = checkNotNull(AppVersion.parseOrNull(name)) { "$name is not a version" }

internal fun published(
    version: String,
    notes: List<PublishedNote> = listOf(note()),
    date: ReleaseDate = ReleaseDate(
        year = 2026,
        month = 11,
        day = 20,
    ),
): PublishedRelease = PublishedRelease(
    version = version(version),
    date = date,
    notes = notes,
)

internal fun note(
    text: String = "Faster sync",
    vararg platforms: String = arrayOf("all"),
): PublishedNote = PublishedNote(
    text = text,
    platforms = platforms.toSet(),
)
