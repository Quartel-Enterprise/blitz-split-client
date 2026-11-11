package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Why: a later time today, which the pickers can't rule out, becomes now, the latest time they reach.
internal fun SplitBillUiState.withSpentAt(
    epochMillis: Long,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val spentAt = minOf(epochMillis, now)
    return copy(
        spentAt = spentAt,
        spentDay = SpentDay.relativeDayOf(
            epochMillis = spentAt,
            nowMillis = now,
            timeZone = timeZone,
        ),
        latestSpentAt = now,
    )
}

/*
 * Why: another day in "Quando"'s calendar keeps the bill's time of day, which is the time it was opened at unless
 * picked otherwise (beta feedback), so a bill never lands on a day at a clock its own day hasn't reached.
 */
internal fun SplitBillUiState.withSpentDayPicked(
    date: LocalDate,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val time = Instant.fromEpochMilliseconds(spentAt).toLocalDateTime(timeZone).time
    return withSpentAt(LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds(), now, timeZone)
}

// Why: the time of day picked for the bill, on the day it has.
internal fun SplitBillUiState.withSpentTimePicked(
    time: LocalTime,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val date = SpentDay.dateOf(spentAt, timeZone)
    return withSpentAt(LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds(), now, timeZone)
}

/*
 * Why: "Hoje" may have become "Ontem" since the screen opened, and the picker may now reach a new day. Opening the
 * picker never moves what was picked, even a time a clock ahead of this device's saved.
 */
internal fun SplitBillUiState.withSpentAtLimitRefreshed(
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState = copy(
    spentDay = SpentDay.relativeDayOf(
        epochMillis = spentAt,
        nowMillis = now,
        timeZone = timeZone,
    ),
    latestSpentAt = now,
)
