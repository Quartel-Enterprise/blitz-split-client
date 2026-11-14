package com.quare.blitzsplit.core.releasenotes.data.parser

import com.quare.blitzsplit.core.releasenotes.data.dto.ReleaseDto
import com.quare.blitzsplit.core.releasenotes.data.dto.ReleaseNotesFeedDto
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedNote
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class ReleaseNotesFeedParserImpl : ReleaseNotesFeedParser {
    // Why: a newer site may add keys this version doesn't know.
    private val feedJson = Json { ignoreUnknownKeys = true }
    private val datePattern = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    private val months = 1..12
    private val days = 1..31

    override fun parse(json: String): List<PublishedRelease>? = try {
        feedJson
            .decodeFromString<ReleaseNotesFeedDto>(json)
            .releases
            .mapNotNull { it.toPublishedOrNull() }
            // Why: the site writes them newest first; the app doesn't count on it.
            .sortedByDescending(PublishedRelease::version)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    // Why: a version named or dated in a way this app can't read is left out, and the others still show.
    private fun ReleaseDto.toPublishedOrNull(): PublishedRelease? {
        val readVersion = AppVersion.parseOrNull(version) ?: return null
        val readDate = parseDate(date) ?: return null
        return PublishedRelease(
            version = readVersion,
            date = readDate,
            notes = notes.map { note ->
                PublishedNote(
                    text = note.text,
                    platforms = note.platforms.toSet(),
                )
            },
        )
    }

    private fun parseDate(text: String): ReleaseDate? = datePattern.matchEntire(text)?.let { match ->
        val (year, month, day) = match.destructured
        ReleaseDate(
            year = year.toInt(),
            month = month.toInt(),
            day = day.toInt(),
        ).takeIf { it.month in months && it.day in days }
    }
}
