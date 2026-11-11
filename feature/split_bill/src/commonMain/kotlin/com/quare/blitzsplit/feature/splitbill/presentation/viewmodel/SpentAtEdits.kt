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

/*
 * Why: a later time today, which the pickers can't rule out, becomes now, the latest time they reach. Whether the bill
 * has a time follows from the instant itself (`SpentDay.hasTime`), so the form shows what saving it stores.
 */
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
        hasSpentTime = SpentDay.hasTime(spentAt, timeZone),
        latestSpentAt = now,
    )
}

// Why: another day in "Quando"'s calendar keeps the bill's time, or its lack of one (beta feedback).
internal fun SplitBillUiState.withSpentDayPicked(
    date: LocalDate,
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState {
    val time = spentTimeOrNull(timeZone)
    val epochMillis = if (time == null) {
        SpentDay.startOf(date, timeZone)
    } else {
        LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
    }
    return withSpentAt(epochMillis, now, timeZone)
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

// Why: "Remover horário": the first instant of the bill's day, which every screen shows as the date alone.
internal fun SplitBillUiState.withSpentTimeCleared(
    now: Long,
    timeZone: TimeZone,
): SplitBillUiState = withSpentAt(SpentDay.startOf(SpentDay.dateOf(spentAt, timeZone), timeZone), now, timeZone)

// Why: a new bill's "Quando" (a duplicate's too): today with no time, its first instant.
internal fun startOfToday(
    now: Long,
    timeZone: TimeZone,
): Long = SpentDay.startOf(SpentDay.dateOf(now, timeZone), timeZone)

// Why: the time of day "Quando" has, null for a bill without one.
internal fun SplitBillUiState.spentTimeOrNull(timeZone: TimeZone): LocalTime? =
    if (hasSpentTime) Instant.fromEpochMilliseconds(spentAt).toLocalDateTime(timeZone).time else null

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
