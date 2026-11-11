package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/*
 * Why: "Quando" (beta feedback): its date and its time, each picked on its own, never after now. A day or a
 * time no calendar has (a UI's mistake) changes nothing.
 */
internal class SpentAtPicking(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
) {
    fun onEvent(event: SplitBillUiEvent.SpentAt) {
        when (event) {
            SplitBillUiEvent.OnSpentAtOpened,
            SplitBillUiEvent.OnSpentTimeOpened,
            -> uiState.update { it.withSpentAtLimitRefreshed(currentTimeMillis(), currentTimeZone()) }

            is SplitBillUiEvent.OnSpentDayPicked -> pickDay(event)

            is SplitBillUiEvent.OnSpentTimePicked -> pickTime(event)
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
}
