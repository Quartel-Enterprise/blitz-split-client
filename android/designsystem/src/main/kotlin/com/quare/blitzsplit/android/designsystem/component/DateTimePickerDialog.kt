package com.quare.blitzsplit.android.designsystem.component

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
import com.quare.blitzsplit.android.designsystem.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/*
 * Why: "Quando" of a bill or a payment picks a date and then a time (Pierre's call), in the device's time zone. The
 * date picker works in UTC days, so the local day goes in and out of it as midnight UTC. Both happened by now: no
 * day after latestMillis's, and the ViewModel makes a later time that day now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    epochMillis: Long,
    latestMillis: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val pickedDateTime = remember(epochMillis, zone) { Instant.ofEpochMilli(epochMillis).atZone(zone) }
    var pickedDayUtcMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val pickedDay = pickedDayUtcMillis
    if (pickedDay == null) {
        val lastDay = remember(latestMillis, zone) { Instant.ofEpochMilli(latestMillis).atZone(zone).toLocalDate() }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = pickedDateTime.toLocalDate().toUtcMillis(),
            selectableDates = remember(lastDay) { UpToDaySelectableDates(lastDay) },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickedDayUtcMillis = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.designsystem_picker_next)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_picker_cancel)) }
            },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = pickedDateTime.hour,
            initialMinute = pickedDateTime.minute,
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
                ) { Text(stringResource(R.string.designsystem_picker_done)) }
            },
            title = { Text(stringResource(R.string.designsystem_when)) },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_picker_cancel)) }
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
