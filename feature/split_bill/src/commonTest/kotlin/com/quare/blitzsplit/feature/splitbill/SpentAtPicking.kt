package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Why: "Quando" picked as a person does, its day in the calendar and then its time, in the ViewModel's time zone.
internal fun SplitBillViewModel.pickSpentAt(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.UTC,
) {
    val picked = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    onEvent(SplitBillUiEvent.OnSpentDayPicked(picked.year, picked.month.ordinal + 1, picked.day))
    onEvent(SplitBillUiEvent.OnSpentTimePicked(picked.hour, picked.minute))
}
