package com.quare.blitzsplit.android.designsystem.component

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.groups.domain.model.ReminderCalendarEvent

private const val EVENT_MINUTES = 15L
private const val MILLIS_PER_MINUTE = 60_000L

/*
 * Why: "Add to calendar" hands the group's repeating reminder to the calendar the person uses (Google Calendar or the
 * system's) as an insert they confirm there; nothing is written without them, and a phone without a calendar says nothing.
 */
@Composable
fun rememberReminderCalendarLauncher(): (ReminderCalendarEvent) -> Unit {
    val context = LocalContext.current
    return remember(context) { { event -> context.addReminderToCalendar(event) } }
}

private fun Context.addReminderToCalendar(event: ReminderCalendarEvent) {
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, getString(R.string.designsystem_reminder_event_title, event.groupName))
        .putExtra(
            CalendarContract.Events.DESCRIPTION,
            getString(R.string.designsystem_reminder_event_description, event.groupName),
        ).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.startMillis + EVENT_MINUTES * MILLIS_PER_MINUTE)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
        .putExtra(CalendarContract.Events.RRULE, event.rrule)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Why: no calendar app on the device, so there is nowhere to add it; the reminder itself still works.
    }
}
