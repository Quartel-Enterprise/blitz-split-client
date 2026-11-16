package com.quare.blitzsplit.android.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.FoldAlertDialog
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.isTwentyFourHour
import com.quare.blitzsplit.android.designsystem.format.rememberIsSystem24HourClock
import com.quare.blitzsplit.android.designsystem.layout.DialogPlacement
import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiEvent
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val SAMPLE_HOUR = 14
private const val SAMPLE_MINUTE = 30

/*
 * Why: Preferences' "Time format", System, 24-hour and 12-hour with a radio each and, under each, how 2:30 in the
 * afternoon reads in it, so the pick shows what it changes; System's example is the device's own clock. A pick closes
 * it, as the language's does.
 */
@Composable
internal fun TimeFormatDialog(
    state: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit,
    onDismiss: () -> Unit,
    placement: DialogPlacement,
) {
    val isSystem24Hour = rememberIsSystem24HourClock()
    FoldAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.profile_time_format),
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                TimeFormatPreference.entries.forEach { option ->
                    TimeFormatOption(
                        option = option,
                        isSelected = option == state.timeFormat,
                        isSystem24Hour = isSystem24Hour,
                        onClick = { onEvent(ProfileUiEvent.OnTimeFormatSelected(option)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        },
        placement = placement,
    )
}

@Composable
private fun TimeFormatOption(
    option: TimeFormatPreference,
    isSelected: Boolean,
    isSystem24Hour: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = rememberSampleTime(option, isSystem24Hour),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Why: 14:30 today, in the option's clock ("14:30" or "2:30 PM", in the UI's language).
@Composable
private fun rememberSampleTime(
    option: TimeFormatPreference,
    isSystem24Hour: Boolean,
): String {
    val locale = LocalLocale.current.platformLocale
    val is24Hour = option.isTwentyFourHour(isSystem24Hour)
    return remember(locale, is24Hour) {
        val zone = ZoneId.systemDefault()
        val sample = LocalDate.now(zone).atTime(LocalTime.of(SAMPLE_HOUR, SAMPLE_MINUTE)).atZone(zone)
        DateTimeTextFormatter(
            locale = locale,
            zone = zone,
            is24Hour = is24Hour,
        ).toTimeText(sample.toInstant().toEpochMilli())
    }
}

@get:StringRes
internal val TimeFormatPreference.labelRes: Int
    get() = when (this) {
        TimeFormatPreference.SYSTEM -> R.string.profile_time_format_system
        TimeFormatPreference.TWENTY_FOUR_HOUR -> R.string.profile_time_format_24_hour
        TimeFormatPreference.TWELVE_HOUR -> R.string.profile_time_format_12_hour
    }
