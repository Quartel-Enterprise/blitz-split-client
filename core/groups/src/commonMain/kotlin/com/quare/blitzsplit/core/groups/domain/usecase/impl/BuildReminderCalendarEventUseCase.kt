package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent
import com.quare.blitzsplit.core.groups.domain.usecase.BuildReminderCalendarEvent
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.datetime.TimeZone

class BuildReminderCalendarEventUseCase(
    private val currentTimeMillis: CurrentTimeMillis,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : BuildReminderCalendarEvent {
    override fun invoke(
        groupName: String,
        reminder: GroupReminder,
    ): ReminderCalendarEvent? = ReminderCalendarEvent.of(
        groupName = groupName,
        reminder = reminder,
        nowMillis = currentTimeMillis(),
        timeZone = timeZone(),
    )
}
