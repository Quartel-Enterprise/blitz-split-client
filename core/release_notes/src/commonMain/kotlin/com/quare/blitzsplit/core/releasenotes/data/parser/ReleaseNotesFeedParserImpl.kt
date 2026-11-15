package com.quare.blitzsplit.core.releasenotes.data.parser

import com.quare.blitzsplit.core.releasenotes.data.dto.ReleaseDto
import com.quare.blitzsplit.core.releasenotes.data.dto.ReleaseNotesFeedDto
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedNote
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import kotlinx.serialization.json.Json

internal class ReleaseNotesFeedParserImpl : ReleaseNotesFeedParser {
    // Why: a newer site may add keys this version doesn't know.
    private val feedJson = Json { ignoreUnknownKeys = true }
    private val datePattern = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    private val months = 1..12

    override fun parse(json: String): List<PublishedRelease>? = try {
        feedJson
            .decodeFromString<ReleaseNotesFeedDto>(json)
            .releases
            .mapNotNull { it.toPublishedOrNull() }
            // Why: the site writes them newest first; the app doesn't count on it.
            .sortedByDescending(PublishedRelease::version)
            // Why: each UI keys a card by its version, so one listed twice ("3.1.0" and "3.01.0") keeps its first.
            .distinctBy(PublishedRelease::version)
    } catch (_: IllegalArgumentException) {
        // Why: kotlinx.serialization's SerializationException is one too.
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
        ).takeIf { it.month in months && it.day in 1..daysIn(it.year, it.month) }
    }

    // Why: a day the month doesn't have ("2026-02-30") would read as another one on the web and on Apple.
    private fun daysIn(
        year: Int,
        month: Int,
    ): Int = when (month) {
        FEBRUARY -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) LEAP_FEBRUARY_DAYS else FEBRUARY_DAYS
        in shortMonths -> SHORT_MONTH_DAYS
        else -> LONG_MONTH_DAYS
    }

    private companion object {
        const val FEBRUARY = 2
        const val FEBRUARY_DAYS = 28
        const val LEAP_FEBRUARY_DAYS = 29
        const val SHORT_MONTH_DAYS = 30
        const val LONG_MONTH_DAYS = 31
        val shortMonths = setOf(4, 6, 9, 11)
    }
}
