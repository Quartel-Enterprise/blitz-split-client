package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent

// Why: the group's reminder as a repeating calendar event starting at its next occurrence on this device's clock.
fun interface BuildReminderCalendarEvent {
    // Why: null while the reminder is off, which has nothing to add to a calendar.
    operator fun invoke(
        groupName: String,
        reminder: GroupReminder,
    ): ReminderCalendarEvent?
}
