package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.android.designsystem.format.rememberReminderTimeText
import com.quare.blitzsplit.android.designsystem.format.rememberWeekdayName
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import java.time.format.TextStyle

private val cardRadius = 20.dp

// Why: what sits under the schedule: New group's switch, Edit group's button, or nothing.
sealed interface ReminderCalendarControl {
    data object None : ReminderCalendarControl

    // Why: New group: on, creating the group also hands the repeating event to the calendar.
    data class Toggle(
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : ReminderCalendarControl

    // Why: Edit group: "Add to calendar" right now.
    data class Button(
        val onClick: () -> Unit,
    ) : ReminderCalendarControl
}

/*
 * Why: "Lembrete do grupo" of New group and Edit group: a switch, then (while on) Weekly or Monthly, the weekday or the
 * day of the month, and the time, read on each member's own clock. Stateless: the ViewModel keeps the schedule.
 */
@Composable
fun GroupReminderSection(
    reminder: GroupReminder,
    onEnabledChange: (Boolean) -> Unit,
    onKindSelected: (ReminderKind) -> Unit,
    onDaySelected: (Int) -> Unit,
    onTimeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    calendar: ReminderCalendarControl = ReminderCalendarControl.None,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        ) {
            SwitchRow(
                title = stringResource(R.string.designsystem_reminder_switch),
                caption = stringResource(R.string.designsystem_reminder_caption),
                isChecked = reminder.isEnabled,
                isEnabled = isEnabled,
                onCheckedChange = onEnabledChange,
                headline = stringResource(R.string.designsystem_reminder_title),
            )
            if (reminder.isEnabled) {
                ReminderScheduleColumn(
                    reminder = reminder,
                    isEnabled = isEnabled,
                    onKindSelected = onKindSelected,
                    onDaySelected = onDaySelected,
                    onTimeSelected = onTimeSelected,
                )
                CalendarControlRow(
                    calendar = calendar,
                    isEnabled = isEnabled,
                )
            }
        }
    }
}

@Composable
private fun ReminderScheduleColumn(
    reminder: GroupReminder,
    isEnabled: Boolean,
    onKindSelected: (ReminderKind) -> Unit,
    onDaySelected: (Int) -> Unit,
    onTimeSelected: (Int) -> Unit,
) {
    var isPickingTime by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val kinds = ReminderKind.entries
        ConnectedButtonRow(
            labels = kinds.map { stringResource(it.labelRes) },
            selectedIndex = kinds.indexOf(reminder.kind),
            groupLabel = stringResource(R.string.designsystem_reminder_title),
            onClick = { if (isEnabled) onKindSelected(kinds[it]) },
            itemRole = Role.RadioButton,
        )
        when (reminder.kind) {
            ReminderKind.WEEKLY -> WeekdayChipRow(
                selectedDay = reminder.day,
                isEnabled = isEnabled,
                onDaySelected = onDaySelected,
            )

            ReminderKind.MONTHLY -> DayOfMonthRow(
                day = reminder.day,
                isEnabled = isEnabled,
                onDaySelected = onDaySelected,
            )
        }
        ValueRow(
            label = stringResource(R.string.designsystem_reminder_time),
            value = rememberReminderTimeText(reminder.minute),
            isEnabled = isEnabled,
            onClick = { isPickingTime = true },
        )
    }
    if (isPickingTime) {
        TimeOfDayPickerDialog(
            initialHour = reminder.hour,
            initialMinute = reminder.minuteOfHour,
            onPicked = { hour, minute ->
                isPickingTime = false
                onTimeSelected(hour * GroupReminder.MINUTES_PER_HOUR + minute)
            },
            onDismiss = { isPickingTime = false },
        )
    }
}

@Composable
private fun WeekdayChipRow(
    selectedDay: Int,
    isEnabled: Boolean,
    onDaySelected: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..GroupReminder.MAX_WEEKDAY).forEach { day ->
            FilterChip(
                selected = day == selectedDay,
                onClick = { onDaySelected(day) },
                enabled = isEnabled,
                label = { Text(rememberWeekdayName(day, TextStyle.SHORT_STANDALONE)) },
            )
        }
    }
}

@Composable
private fun DayOfMonthRow(
    day: Int,
    isEnabled: Boolean,
    onDaySelected: (Int) -> Unit,
) {
    var isMenuOpen by rememberSaveable { mutableStateOf(false) }
    Box {
        ValueRow(
            label = stringResource(R.string.designsystem_reminder_day_of_month),
            value = day.toString(),
            isEnabled = isEnabled,
            onClick = { isMenuOpen = true },
            showsArrow = true,
        )
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            (1..GroupReminder.MAX_MONTH_DAY).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        isMenuOpen = false
                        onDaySelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    showsArrow: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showsArrow) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalendarControlRow(
    calendar: ReminderCalendarControl,
    isEnabled: Boolean,
) {
    when (calendar) {
        ReminderCalendarControl.None -> Unit

        is ReminderCalendarControl.Toggle -> SwitchRow(
            title = stringResource(R.string.designsystem_reminder_calendar_new),
            caption = stringResource(R.string.designsystem_reminder_calendar_new_caption),
            isChecked = calendar.isChecked,
            isEnabled = isEnabled,
            onCheckedChange = calendar.onCheckedChange,
        )

        is ReminderCalendarControl.Button -> TextButton(
            onClick = calendar.onClick,
            enabled = isEnabled,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.designsystem_reminder_calendar),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// Why: one tappable row for the whole switch, so the label is the target too.
@Composable
private fun SwitchRow(
    title: String,
    caption: String,
    isChecked: Boolean,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    headline: String? = null,
) {
    Column {
        if (headline != null) {
            Text(
                text = headline,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isChecked,
                onCheckedChange = null,
                enabled = isEnabled,
            )
        }
    }
}

private val ReminderKind.labelRes: Int
    get() = when (this) {
        ReminderKind.WEEKLY -> R.string.designsystem_reminder_weekly
        ReminderKind.MONTHLY -> R.string.designsystem_reminder_monthly
    }
