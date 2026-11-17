package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.groups.domain.model.GroupReminder
import com.quare.blitzsplit.core.groups.domain.model.ReminderKind
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle

/*
 * Why: the reminder's time of day in the person's clock (12 or 24 hours), "10:00" or "10:00 AM", with the locale's
 * own pattern every other time in the app uses (Korean and Japanese put the AM/PM marker first).
 */
@Composable
fun rememberReminderTimeText(minute: Int): String {
    val formatter = rememberDateTimeTextFormatter()
    return remember(minute, formatter) {
        formatter.toTimeText(
            LocalTime.of(minute / GroupReminder.MINUTES_PER_HOUR, minute % GroupReminder.MINUTES_PER_HOUR),
        )
    }
}

// Why: the weekday's full name in the app's language, from the ISO number the server keeps (1 Monday to 7 Sunday).
@Composable
fun rememberWeekdayName(
    isoDay: Int,
    style: TextStyle = TextStyle.FULL_STANDALONE,
): String {
    val locale = LocalLocale.current.platformLocale
    return remember(isoDay, locale, style) { DayOfWeek.of(isoDay).getDisplayName(style, locale) }
}

// Why: "Every Monday at 10:00" or "Every month on day 25 at 12:00", the banner's and the calendar's line.
@Composable
fun reminderScheduleText(reminder: GroupReminder): String {
    val time = rememberReminderTimeText(reminder.minute)
    return when (reminder.kind) {
        ReminderKind.WEEKLY -> stringResource(
            R.string.designsystem_reminder_schedule_weekly,
            rememberWeekdayName(reminder.day, TextStyle.FULL),
            time,
        )

        ReminderKind.MONTHLY -> stringResource(R.string.designsystem_reminder_schedule_monthly, reminder.day, time)
    }
}
