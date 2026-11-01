package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private const val NOON = 12

/*
 * Why: "Escolher datas…", from one day to another, never after today. The picker works in UTC days, so each local day
 * goes in and out of it as midnight UTC, and the ViewModel gets noon of each day in the device's time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsDateRangeDialog(
    startMillis: Long,
    endMillis: Long,
    onPicked: (Long, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember(zone) { LocalDate.now(zone) }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = Instant
            .ofEpochMilli(startMillis)
            .atZone(zone)
            .toLocalDate()
            .toUtcMillis(),
        initialSelectedEndDateMillis = Instant
            .ofEpochMilli(endMillis)
            .atZone(zone)
            .toLocalDate()
            .toUtcMillis(),
        selectableDates = remember(today) { UpToTodaySelectableDates(today) },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis ?: return@TextButton
                    val end = state.selectedEndDateMillis ?: start
                    onPicked(start.toLocalNoon(zone), end.toLocalNoon(zone))
                },
                enabled = state.selectedStartDateMillis != null,
            ) { Text(stringResource(R.string.group_stats_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.group_stats_cancel)) }
        },
    ) {
        DateRangePicker(state = state)
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalNoon(zone: ZoneId): Long = Instant
    .ofEpochMilli(this)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .atTime(NOON, 0)
    .atZone(zone)
    .toInstant()
    .toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
private class UpToTodaySelectableDates(
    private val today: LocalDate,
) : SelectableDates {
    private val todayUtcMillis = today.toUtcMillis()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis

    override fun isSelectableYear(year: Int): Boolean = year <= today.year
}
