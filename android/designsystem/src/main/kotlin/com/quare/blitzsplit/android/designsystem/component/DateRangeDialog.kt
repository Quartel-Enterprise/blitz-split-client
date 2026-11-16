package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private const val NOON = 12

/*
 * Why: "Pick dates…" of Statistics and "Custom range…" of All expenses, from one day to another, never after today. The picker works in UTC days, so each local day
 * goes in and out of it as midnight UTC, and the ViewModel gets noon of each day in the device's time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeDialog(
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
            ) { Text(stringResource(R.string.designsystem_range_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_picker_cancel)) }
        },
    ) {
        val formatter = remember { DatePickerDefaults.dateFormatter() }
        DateRangePicker(
            state = state,
            dateFormatter = formatter,
            headline = { RangeHeadlineText(state, formatter) },
        )
    }
}

/*
 * Why: Material's headline keeps both dates in one row, so on a phone the end date ("14 de nov. de 2026") wrapped a
 * letter or two per line. Here each date stays whole and the end date goes under the start one when both don't fit,
 * with Material's padding and the text style the picker gives its headline.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RangeHeadlineText(
    state: DateRangePickerState,
    formatter: DatePickerFormatter,
) {
    val locale = LocalConfiguration.current.locales[0]
    val startLabel = stringResource(R.string.designsystem_range_start)
    val endLabel = stringResource(R.string.designsystem_range_end)
    val none = stringResource(R.string.designsystem_range_none)
    val start = formatter.formatDate(state.selectedStartDateMillis, locale) ?: startLabel
    val end = formatter.formatDate(state.selectedEndDateMillis, locale) ?: endLabel
    // Why: as Material's headline, TalkBack reads both dates in full with what each is, and again as they change.
    val description = listOf(
        startLabel to state.selectedStartDateMillis,
        endLabel to state.selectedEndDateMillis,
    ).joinToString { (label, millis) ->
        "$label: ${formatter.formatDate(millis, locale, forContentDescription = true) ?: none}"
    }
    FlowRow(
        modifier = Modifier
            .padding(
                start = 64.dp,
                end = 12.dp,
                bottom = 12.dp,
            ).clearAndSetSemantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = description
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "$start –")
        Text(text = end)
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
