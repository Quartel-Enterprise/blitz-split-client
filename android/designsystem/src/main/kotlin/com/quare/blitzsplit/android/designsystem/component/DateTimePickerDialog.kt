package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.android.designsystem.layout.FoldDialog
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

// Why: the picker, and the book's half it needs with FoldDialog's 16dp margin on each side of it.
private val pickerWidth = 360.dp
private val bookHalfMinWidth = pickerWidth + 32.dp

/*
 * Why: "Quando" of a payment picks a date and then a time (Pierre's call), in the device's time zone. Both happened by
 * now: no day after latestMillis's, and the ViewModel makes a later time that day now.
 */
@Composable
fun DateTimePickerDialog(
    epochMillis: Long,
    latestMillis: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val pickedDateTime = remember(epochMillis, zone) { Instant.ofEpochMilli(epochMillis).atZone(zone) }
    var pickedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val pickedDay = pickedEpochDay
    if (pickedDay == null) {
        DayPickerDialog(
            epochMillis = epochMillis,
            latestMillis = latestMillis,
            confirmLabel = stringResource(R.string.designsystem_picker_next),
            onPicked = { pickedEpochDay = it.toEpochDay() },
            onDismiss = onDismiss,
        )
    } else {
        TimeOfDayPickerDialog(
            initialHour = pickedDateTime.hour,
            initialMinute = pickedDateTime.minute,
            onPicked = { hour, minute ->
                val millis = LocalDate
                    .ofEpochDay(pickedDay)
                    .atTime(LocalTime.of(hour, minute))
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                onPicked(millis)
            },
            onDismiss = onDismiss,
        )
    }
}

/*
 * Why: a day of the device's calendar, no later than latestMillis's: a bill's "Quando" picks its date here, its time
 * with its own control, never chained (beta feedback). The Material picker works in UTC days, so the local day goes in and out of it as midnight UTC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(
    epochMillis: Long,
    latestMillis: Long,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.designsystem_picker_done),
) {
    val zone = remember { ZoneId.systemDefault() }
    val isInBookHalf = rememberIsPickerInBookHalf()
    val lastDay = remember(latestMillis, zone) { Instant.ofEpochMilli(latestMillis).atZone(zone).toLocalDate() }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = remember(epochMillis, zone) {
            Instant
                .ofEpochMilli(epochMillis)
                .atZone(zone)
                .toLocalDate()
                .toUtcMillis()
        },
        selectableDates = remember(lastDay) { UpToDaySelectableDates(lastDay) },
    )
    val dismiss: @Composable () -> Unit = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_picker_cancel)) }
    }
    val confirm: @Composable () -> Unit = {
        TextButton(
            onClick = {
                dateState.selectedDateMillis?.let { utcMillis ->
                    onPicked(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            },
            enabled = dateState.selectedDateMillis != null,
        ) { Text(confirmLabel) }
    }
    if (isInBookHalf) {
        BookHalfPickerDialog(
            onDismiss = onDismiss,
            title = null,
            confirmButton = confirm,
            dismissButton = dismiss,
        ) {
            DatePicker(state = dateState)
        }
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = confirm,
            dismissButton = dismiss,
        ) {
            DatePicker(state = dateState)
        }
    }
}

// Why: a time of day on the clock, "Quando"'s second step for a payment and a bill's time.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOfDayPickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onPicked: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val isInBookHalf = rememberIsPickerInBookHalf()
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
    )
    val dismiss: @Composable () -> Unit = {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.designsystem_picker_cancel)) }
    }
    val confirm: @Composable () -> Unit = {
        TextButton(
            onClick = { onPicked(timeState.hour, timeState.minute) },
        ) { Text(stringResource(R.string.designsystem_picker_done)) }
    }
    if (isInBookHalf) {
        BookHalfPickerDialog(
            onDismiss = onDismiss,
            title = stringResource(R.string.designsystem_when),
            confirmButton = confirm,
            dismissButton = dismiss,
        ) {
            // Why: a book lying down would pick the clock beside its inputs, wider than the half's 360dp.
            TimePicker(
                state = timeState,
                modifier = Modifier.padding(top = 8.dp),
                layoutType = TimePickerLayoutType.Vertical,
            )
        }
    } else {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = confirm,
            title = { Text(stringResource(R.string.designsystem_when)) },
            dismissButton = dismiss,
        ) {
            TimePicker(
                state = timeState,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/*
 * Why: a book draws the pickers in the half the bill or the payment is in, off the hinge, where that half holds the
 * calendar's 360dp (a Galaxy Z Fold's narrower halves keep it centered); a tabletop's half is too short for a
 * calendar, so they stay centered there.
 */
@Composable
private fun rememberIsPickerInBookHalf(): Boolean {
    val fold = rememberFoldLayout()
    val bookSplit = fold.bookSplit
    return bookSplit != null &&
        fold.tabletopSplit == null &&
        min(
            bookSplit.bandStart - fold.windowBounds.left,
            fold.windowBounds.right - bookSplit.bandEnd,
        ) >= bookHalfMinWidth
}

// Why: the Material picker dialog's surface and buttons, in a book's end half.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookHalfPickerDialog(
    onDismiss: () -> Unit,
    title: String?,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    FoldDialog(
        onDismissRequest = onDismiss,
        placement = DialogPlacement.EndHalf,
    ) {
        Surface(
            modifier = Modifier.width(pickerWidth),
            shape = DatePickerDefaults.shape,
            color = DatePickerDefaults.colors().containerColor,
            tonalElevation = DatePickerDefaults.TonalElevation,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                title?.let {
                    Text(
                        text = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 24.dp,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                content()
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(
                            end = 6.dp,
                            bottom = 8.dp,
                        ),
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
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
