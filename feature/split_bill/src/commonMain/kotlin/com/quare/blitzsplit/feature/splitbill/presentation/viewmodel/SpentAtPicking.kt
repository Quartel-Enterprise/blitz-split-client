package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * Why: "Quando" (beta feedback): its date and its optional time, each picked on its own, never after now. A day or a
 * time no calendar has (a UI's mistake) changes nothing.
 */
internal class SpentAtPicking(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val trackEvent: TrackEvent,
) {
    fun onEvent(event: SplitBillUiEvent) {
        when (event) {
            SplitBillUiEvent.OnSpentAtOpened,
            SplitBillUiEvent.OnSpentTimeOpened,
            -> uiState.update { it.withSpentAtLimitRefreshed(currentTimeMillis(), currentTimeZone()) }

            is SplitBillUiEvent.OnSpentDayPicked -> pickDay(event)

            is SplitBillUiEvent.OnSpentTimePicked -> pickTime(event)

            SplitBillUiEvent.OnSpentTimeAdded -> addTime()

            SplitBillUiEvent.OnSpentTimeCleared -> clearTime()

            else -> Unit
        }
    }

    private fun pickDay(event: SplitBillUiEvent.OnSpentDayPicked) {
        val date = runCatching { LocalDate(event.year, event.month, event.day) }.getOrNull() ?: return
        uiState.update { it.withSpentDayPicked(date, currentTimeMillis(), currentTimeZone()) }
    }

    private fun pickTime(event: SplitBillUiEvent.OnSpentTimePicked) {
        val time = runCatching { LocalTime(event.hour, event.minute) }.getOrNull() ?: return
        uiState.update { it.withSpentTimePicked(time, currentTimeMillis(), currentTimeZone()) }
    }

    /*
     * Why: the time it is now (by this ViewModel's clock), to the minute, on the bill's day; in the first minute of
     * the day that is 00:00, which reads as no time, so it takes 00:01 instead.
     */
    private fun addTime() {
        val now = currentTimeMillis()
        val timeZone = currentTimeZone()
        val clock = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone).time
        val time = if (clock.hour == 0 && clock.minute == 0) LocalTime(0, 1) else LocalTime(clock.hour, clock.minute)
        uiState.update { it.withSpentAtLimitRefreshed(now, timeZone).withSpentTimePicked(time, now, timeZone) }
    }

    private fun clearTime() {
        if (!uiState.value.hasSpentTime) return
        trackEvent("split_bill_time_cleared", emptyMap())
        uiState.update { it.withSpentTimeCleared(currentTimeMillis(), currentTimeZone()) }
    }
}
