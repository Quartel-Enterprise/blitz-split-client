package com.quare.blitzsplit.feature.creategroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent

sealed interface CreateGroupUiAction {
    // Why: the group was created with "Add to my calendar" on: the UI hands the repeating event to the device's calendar.
    data class AddReminderToCalendar(
        val event: ReminderCalendarEvent,
    ) : CreateGroupUiAction
}
