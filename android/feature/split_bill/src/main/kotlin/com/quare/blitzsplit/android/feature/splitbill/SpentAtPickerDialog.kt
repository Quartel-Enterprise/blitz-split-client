package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/*
 * Why: "Quando" picks a date and then a time (Pierre's call), in the device's time zone. The date picker works in
 * UTC days, so the bill's local day goes in and out of it as midnight UTC. A bill is spent by now: no day after
 * latestSpentAt's, and the ViewModel makes a later time that day now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpentAtPickerDialog(
    spentAt: Long,
    latestSpentAt: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val spentDateTime = remember(spentAt, zone) { Instant.ofEpochMilli(spentAt).atZone(zone) }
    var pickedDayUtcMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val pickedDay = pickedDayUtcMillis
    if (pickedDay == null) {
        val lastDay = remember(latestSpentAt, zone) { Instant.ofEpochMilli(latestSpentAt).atZone(zone).toLocalDate() }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = spentDateTime.toLocalDate().toUtcMillis(),
            selectableDates = remember(lastDay) { UpToDaySelectableDates(lastDay) },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickedDayUtcMillis = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.split_bill_next)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.split_bill_cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = spentDateTime.hour,
            initialMinute = spentDateTime.minute,
        )
        TimePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val day = Instant.ofEpochMilli(pickedDay).atZone(ZoneOffset.UTC).toLocalDate()
                        val millis = day
                            .atTime(LocalTime.of(timeState.hour, timeState.minute))
                            .atZone(zone)
                            .toInstant()
                            .toEpochMilli()
                        onPicked(millis)
                    },
                ) { Text(stringResource(R.string.split_bill_done)) }
            },
            title = { Text(stringResource(R.string.split_bill_when)) },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.split_bill_cancel)) }
            },
        ) {
            TimePicker(
                state = timeState,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
private class UpToDaySelectableDates(
    private val lastDay: LocalDate,
) : SelectableDates {
    private val lastDayUtcMillis = lastDay.toUtcMillis()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= lastDayUtcMillis

    override fun isSelectableYear(year: Int): Boolean = year <= lastDay.year
}
