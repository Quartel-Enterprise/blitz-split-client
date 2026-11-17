package com.quare.blitzsplit.android.feature.creategroup

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupReminderSection
import com.quare.blitzsplit.android.designsystem.component.ReminderCalendarControl
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiState

// Why: "Lembrete do grupo" under the currency: Monday 10:00 unless changed, with "Add to my calendar" for the new group.
@Composable
internal fun ReminderRow(
    state: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
) {
    GroupReminderSection(
        reminder = state.reminder,
        onEnabledChange = { onEvent(CreateGroupUiEvent.OnReminderEnabledChanged(it)) },
        onKindSelected = { onEvent(CreateGroupUiEvent.OnReminderKindSelected(it)) },
        onDaySelected = { onEvent(CreateGroupUiEvent.OnReminderDaySelected(it)) },
        onTimeSelected = { onEvent(CreateGroupUiEvent.OnReminderTimeSelected(it)) },
        isEnabled = !state.isCreating,
        calendar = ReminderCalendarControl.Toggle(
            isChecked = state.addsReminderToCalendar,
            onCheckedChange = { onEvent(CreateGroupUiEvent.OnAddToCalendarChanged(it)) },
        ),
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 16.dp,
        ),
    )
}
