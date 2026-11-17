package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent

sealed interface EditGroupUiAction {
    // Why: "Add to calendar": the UI hands the repeating event to the device's calendar.
    data class AddReminderToCalendar(
        val event: ReminderCalendarEvent,
    ) : EditGroupUiAction
}
