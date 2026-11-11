package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quare.blitzsplit.android.designsystem.component.DayPickerDialog
import com.quare.blitzsplit.android.designsystem.component.TimeOfDayPickerDialog
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import java.time.Instant
import java.time.ZoneId

// Why: "Quando"'s two pickers, its date and its time, each opened on its own (beta feedback).
internal enum class SpentAtPicker {
    DAY,
    TIME,
}

@Composable
internal fun SpentAtPickerDialog(
    picker: SpentAtPicker?,
    state: SplitBillUiState,
    onClose: () -> Unit,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    when (picker) {
        null -> Unit

        SpentAtPicker.DAY -> DayPickerDialog(
            epochMillis = state.spentAt,
            latestMillis = state.latestSpentAt,
            onPicked = { date ->
                onClose()
                onEvent(SplitBillUiEvent.OnSpentDayPicked(date.year, date.monthValue, date.dayOfMonth))
            },
            onDismiss = onClose,
        )

        SpentAtPicker.TIME -> {
            val initial = remember(state.spentAt) {
                Instant.ofEpochMilli(state.spentAt).atZone(ZoneId.systemDefault())
            }
            TimeOfDayPickerDialog(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                onPicked = { hour, minute ->
                    onClose()
                    onEvent(SplitBillUiEvent.OnSpentTimePicked(hour, minute))
                },
                onDismiss = onClose,
            )
        }
    }
}
