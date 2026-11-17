package com.quare.blitzsplit.core.groups.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

// Why: the epoch millis of a wall clock time in a zone, so the reminder tests read as dates and not as numbers.
internal fun millisAt(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    timeZone: TimeZone = TimeZone.UTC,
): Long = LocalDateTime(year, month, day, hour, minute).toInstant(timeZone).toEpochMilliseconds()
